package com.ctrip;

import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.Emulator;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.IO;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


// 1、继承关系
public class ctrip extends AbstractJni implements IOResolver{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    ctrip() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.xiaochuankeji.tieba").build(); // 创建模拟器实例
        //2、 绑定IO重定向接口
        emulator.getSyscallHandler().addIOResolver(this);
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\ctrip\\xc 8-38-2.apk")); // 创建Android虚拟机
        new AndroidModule(emulator, vm).register(memory);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\ctrip\\libscmain.so"), true); // 加载so到虚拟内存
        module = dm.getModule(); //获取本SO模块的句柄

        vm.setJni(this);
        vm.setVerbose(true);
        dm.callJNI_OnLoad(emulator);
    }


    public void callsimpleSign() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        String input = "7be9f13e7f5426d139cb4e5dbb1fdba7";
        byte[] inputbyte = input.getBytes(StandardCharsets.UTF_8);
        ByteArray inputbytearry = new ByteArray(vm, inputbyte);
        list.add(vm.addLocalObject(inputbytearry));
        list.add(vm.addLocalObject(new StringObject(vm, "getdata")));
        Number number = module.callFunction(emulator, 0x869d9, list.toArray())[0];
        System.out.println(vm.getObject(number.intValue()).getValue().toString());

    }

    public static void main(String[] args) throws Exception {
        ctrip test = new ctrip();
        test.callsimpleSign();
    }

    //3、
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags){
        System.out.println("访问：" + pathname);
        if (("proc/" + emulator.getPid() + "/cmdline").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "ctrip.android.viewroot".getBytes()));
        }
        if (("proc/" + emulator.getPid() + "/status").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname,
                    ("Name:   ip.android.view\n" +
                    "State:  S (sleeping)\n" +
                    "Tgid:   17334\n" +
                    "Pid:    17334\n" +
                    "PPid:   3988\n" +
                    "TracerPid:      0\n" +
                    "Uid:    10165   10165   10165   10165\n" +
                    "Gid:    10165   10165   10165   10165\n" +
                    "FDSize: 512\n" +
                    "Groups: 3002 3003 9997 50165\n" +
                    "VmPeak:  2750476 kB\n" +
                    "VmSize:  2669768 kB\n" +
                    "VmLck:         0 kB\n" +
                    "VmPin:         0 kB\n" +
                    "VmHWM:    625440 kB\n" +
                    "VmRSS:    551996 kB\n" +
                    "VmData:   635512 kB\n" +
                    "VmStk:      8196 kB\n" +
                    "VmExe:        48 kB\n" +
                    "VmLib:    231276 kB\n" +
                    "VmPTE:      3056 kB\n" +
                    "VmSwap:    16756 kB\n" +
                    "Threads:        177\n" +
                    "SigQ:   6/9061\n" +
                    "SigPnd: 0000000000000000\n" +
                    "ShdPnd: 0000000000000000\n" +
                    "SigBlk: 0000000000001204\n" +
                    "SigIgn: 0000000000000000\n" +
                    "SigCgt: 00000002400096f8\n" +
                    "CapInh: 0000000000000000\n" +
                    "CapPrm: 0000000000000000\n" +
                    "CapEff: 0000000000000000\n" +
                    "CapBnd: 0000000000000000\n" +
                    "Seccomp:        0\n" +
                    "Cpus_allowed:   7f\n" +
                    "Cpus_allowed_list:      0-6\n" +
                    "Mems_allowed:   1\n" +
                    "Mems_allowed_list:      0\n" +
                    "voluntary_ctxt_switches:        1233861\n" +
                    "nonvoluntary_ctxt_switches:     323282").getBytes()));
        }
        return null;
    }
}