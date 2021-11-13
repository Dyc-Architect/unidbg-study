package com.github.unidbg.linux.android;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Svc;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.ArmHook;
import com.github.unidbg.arm.ArmSvc;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.linux.LinuxModule;
import com.github.unidbg.linux.struct.dl_phdr_info;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.pointer.UnidbgStructure;
import com.github.unidbg.spi.Dlfcn;
import com.github.unidbg.spi.InitFunction;
import com.github.unidbg.unix.struct.DlInfo;
import com.sun.jna.Pointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import net.fornwall.jelf.ElfDynamicStructure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.ArmConst;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArmLD extends Dlfcn {

    private static final Log log = LogFactory.getLog(ArmLD.class);

    private final Backend backend;

    ArmLD(Backend backend, SvcMemory svcMemory) {
        super(svcMemory);
        this.backend = backend;
    }

    @Override
    public long hook(final SvcMemory svcMemory, String libraryName, String symbolName, long old) {
        if ("libdl.so".equals(libraryName)) {
            if (log.isDebugEnabled()) {
                log.debug("link " + symbolName + ", old=0x" + Long.toHexString(old));
            }
            switch (symbolName) {
                case "dl_iterate_phdr":
                    return svcMemory.registerSvc(new ArmSvc() {
                        private MemoryBlock block;
                        @Override
                        public UnidbgPointer onRegister(SvcMemory svcMemory, int svcNumber) {
                            try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
                                KeystoneEncoded encoded = keystone.assemble(Arrays.asList(
                                        "push {r4-r7, lr}",
                                        "svc #0x" + Integer.toHexString(svcNumber),
                                        "pop {r7}",
                                        "cmp r7, #0",
                                        "beq 0x34",
                                        "pop {r0-r2}",
                                        "blx r7",
                                        "cmp r0, #0",
                                        "beq 0x8",
                                        "pop {r7}",
                                        "cmp r7, #0",
                                        "popne {r4-r6}",
                                        "bne 0x24",
                                        "mov r7, #0",
                                        "mov r5, #0x" + Integer.toHexString(Svc.CALLBACK_SYSCALL_NUMBER),
                                        "mov r4, #0x" + Integer.toHexString(svcNumber),
                                        "svc #0",
                                        "pop {r4-r7, pc}"));
                                byte[] code = encoded.getMachineCode();
                                UnidbgPointer pointer = svcMemory.allocate(code.length, "dl_iterate_phdr");
                                pointer.write(0, code, 0, code.length);
                                if (log.isDebugEnabled()) {
                                    log.debug("dl_iterate_phdr: pointer=" + pointer);
                                }
                                return pointer;
                            }
                        }
                        @Override
                        public long handle(Emulator<?> emulator) {
                            if (block != null) {
                                throw new IllegalStateException();
                            }

                            RegisterContext context = emulator.getContext();
                            UnidbgPointer cb = context.getPointerArg(0);
                            UnidbgPointer data = context.getPointerArg(1);

                            Collection<Module> modules = emulator.getMemory().getLoadedModules();
                            List<LinuxModule> list = new ArrayList<>();
                            for (Module module : modules) {
                                LinuxModule lm = (LinuxModule) module;
                                if (lm.elfFile != null) {
                                    list.add(lm);
                                }
                            }
                            Collections.reverse(list);
                            final int size = UnidbgStructure.calculateSize(dl_phdr_info.class);
                            block = emulator.getMemory().malloc(size * list.size(), true);
                            UnidbgPointer ptr = block.getPointer();
                            Backend backend = emulator.getBackend();
                            UnidbgPointer sp = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
                            if (log.isDebugEnabled()) {
                                log.debug("dl_iterate_phdr cb=" + cb + ", data=" + data + ", size=" + list.size() + ", sp=" + sp);
                            }

                            try {
                                sp = sp.share(-4, 0);
                                sp.setInt(0, 0); // NULL-terminated

                                for (LinuxModule module : list) {
                                    dl_phdr_info info = new dl_phdr_info(ptr);
                                    info.dlpi_addr = UnidbgPointer.pointer(emulator, module.base);
                                    assert info.dlpi_addr != null;
                                    ElfDynamicStructure dynamicStructure = module.dynamicStructure;
                                    if (dynamicStructure != null && dynamicStructure.soName > 0 && dynamicStructure.dt_strtab_offset > 0) {
                                        info.dlpi_name = info.dlpi_addr.share(dynamicStructure.dt_strtab_offset + dynamicStructure.soName);
                                    } else {
                                        info.dlpi_name = module.createPathMemory(svcMemory);
                                    }
                                    info.dlpi_phdr = info.dlpi_addr.share(module.elfFile.ph_offset);
                                    info.dlpi_phnum = module.elfFile.num_ph;
                                    info.pack();

                                    sp = sp.share(-4, 0);
                                    sp.setPointer(0, data); // data

                                    sp = sp.share(-4, 0);
                                    sp.setInt(0, size); // size

                                    sp = sp.share(-4, 0);
                                    sp.setPointer(0, ptr); // dl_phdr_info

                                    sp = sp.share(-4, 0);
                                    sp.setPointer(0, cb); // callback

                                    ptr = ptr.share(size, 0);
                                }

                                return context.getLongArg(0);
                            } finally {
                                backend.reg_write(ArmConst.UC_ARM_REG_SP, sp.peer);
                            }
                        }
                        @Override
                        public void handleCallback(Emulator<?> emulator) {
                            super.handleCallback(emulator);

                            if (block == null) {
                                throw new IllegalStateException();
                            }
                            block.free();
                            block = null;
                        }
                    }).peer;
                case "dlerror":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public long handle(Emulator<?> emulator) {
                            return error.peer;
                        }
                    }).peer;
                case "dlclose":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public long handle(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            int handle = context.getIntArg(0);
                            if (log.isDebugEnabled()) {
                                log.debug("dlclose handle=0x" + Long.toHexString(handle));
                            }
                            return dlclose(emulator.getMemory(), handle);
                        }
                    }).peer;
                case "dlopen":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public UnidbgPointer onRegister(SvcMemory svcMemory, int svcNumber) {
                            ByteBuffer buffer = ByteBuffer.allocate(28);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            buffer.putInt(0xe92d40f0); // push {r4-r7, lr}
                            buffer.putInt(ArmSvc.assembleSvc(svcNumber)); // svc #svcNumber
                            buffer.putInt(0xe49d7004); // pop {r7}
                            buffer.putInt(0xe3570000); // cmp r7, #0
                            buffer.putInt(0x124fe010); // subne lr, pc, #16
                            buffer.putInt(0x112fff17); // bxne r7
                            buffer.putInt(0xe8bd80f1); // pop {r0, r4-r7, pc} with return address
                            byte[] code = buffer.array();
                            UnidbgPointer pointer = svcMemory.allocate(code.length, "dlopen");
                            pointer.write(code);
                            return pointer;
                        }
                        @Override
                        public long handle(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            Pointer filename = context.getPointerArg(0);
                            int flags = context.getIntArg(1);
                            if (log.isDebugEnabled()) {
                                log.debug("dlopen filename=" + filename.getString(0) + ", flags=" + flags);
                            }
                            return dlopen(emulator.getMemory(), filename.getString(0), emulator);
                        }
                    }).peer;
                case "dladdr":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public long handle(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            int addr = context.getIntArg(0);
                            Pointer info = context.getPointerArg(1);
                            if (log.isDebugEnabled()) {
                                log.debug("dladdr addr=0x" + Long.toHexString(addr) + ", info=" + info);
                            }
                            Module module = emulator.getMemory().findModuleByAddress(addr);
                            if (module == null) {
                                return 0;
                            }

                            Symbol symbol = module.findClosestSymbolByAddress(addr, true);

                            DlInfo dlInfo = new DlInfo(info);
                            dlInfo.dli_fname = module.createPathMemory(svcMemory);
                            dlInfo.dli_fbase = UnidbgPointer.pointer(emulator, module.base);
                            if (symbol != null) {
                                dlInfo.dli_sname = symbol.createNameMemory(svcMemory);
                                dlInfo.dli_saddr = UnidbgPointer.pointer(emulator, symbol.getAddress());
                            }
                            dlInfo.pack();
                            return 1;
                        }
                    }).peer;
                case "dlsym":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public long handle(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            int handle = context.getIntArg(0);
                            Pointer symbol = context.getPointerArg(1);
                            if (log.isDebugEnabled()) {
                                log.debug("dlsym handle=0x" + Long.toHexString(handle) + ", symbol=" + symbol.getString(0));
                            }
                            return dlsym(emulator, handle, symbol.getString(0));
                        }
                    }).peer;
                case "dl_unwind_find_exidx":
                    return svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public long handle(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            Pointer pc = context.getPointerArg(0);
                            Pointer pcount = context.getPointerArg(1);
                            log.info("dl_unwind_find_exidx pc" + pc + ", pcount=" + pcount);
                            return 0;
                        }
                    }).peer;
                case "android_get_application_target_sdk_version":
                    return svcMemory.registerSvc(new ArmHook() {
                        @Override
                        protected HookStatus hook(Emulator<?> emulator) {
                            return HookStatus.LR(emulator, 0);
                        }
                    }).peer;
            }
        }
        return 0;
    }

    private long dlopen(Memory memory, String filename, Emulator<?> emulator) {
        Pointer pointer = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
        try {
            Module module = memory.dlopen(filename, false);
            pointer = pointer.share(-4); // return value
            if (module == null) {
                pointer.setInt(0, 0);

                pointer = pointer.share(-4); // NULL-terminated
                pointer.setInt(0, 0);

                if (!"libnetd_client.so".equals(filename)) {
                    log.info("dlopen failed: " + filename + ", LR=" + emulator.getContext().getLRPointer());
                } else if(log.isDebugEnabled()) {
                    log.debug("dlopen failed: " + filename);
                }
                this.error.setString(0, "Resolve library " + filename + " failed");
                return 0;
            } else {
                pointer.setInt(0, (int) module.base);

                pointer = pointer.share(-4); // NULL-terminated
                pointer.setInt(0, 0);

                for (Module md : memory.getLoadedModules()) {
                    LinuxModule m = (LinuxModule) md;
                    if (!m.getUnresolvedSymbol().isEmpty()) {
                        continue;
                    }
                    for (InitFunction initFunction : m.initFunctionList) {
                        long address = initFunction.getAddress();
                        if (address == 0) {
                            continue;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("[" + m.name + "]PushInitFunction: 0x" + Long.toHexString(address));
                        }
                        pointer = pointer.share(-4); // init array
                        pointer.setInt(0, (int) address);
                    }
                    m.initFunctionList.clear();
                }

                return module.base;
            }
        } finally {
            backend.reg_write(ArmConst.UC_ARM_REG_SP, ((UnidbgPointer) pointer).peer);
        }
    }

    private int dlclose(Memory memory, long handle) {
        if (memory.dlclose(handle)) {
            return 0;
        } else {
            this.error.setString(0, "dlclose 0x" + Long.toHexString(handle) + " failed");
            return -1;
        }
    }

}
