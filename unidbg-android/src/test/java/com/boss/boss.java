package com.boss;

import com.github.unidbg.Emulator;
import com.github.unidbg.StringNumber;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.ApplicationInfo;
import com.github.unidbg.linux.android.dvm.api.ClassLoader;
import com.github.unidbg.linux.android.dvm.api.PackageInfo;
import com.github.unidbg.linux.android.dvm.api.Signature;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.github.unidbg.linux.android.dvm.array.ArrayObject.newStringArray;


public class boss extends AbstractJni{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    boss() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.hpbr.bosszhipin").build(); // 创建模拟器实例
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\boss\\boss_9.100_c0.apk")); // 创建Android虚拟机
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\boss\\libyzwg.so"), true); // 加载so到虚拟内存
        module = dm.getModule(); //获取本SO模块的句柄

        vm.setJni(this);
        vm.setVerbose(true);
        dm.callJNI_OnLoad(emulator);
        System.out.println();
    };

    public String callDecodeContent(){
        System.out.println("进入");
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        ByteArray plaintext = new ByteArray(vm, "r0ysue".getBytes(StandardCharsets.UTF_8));
        list.add(vm.addLocalObject(plaintext));
//        String key = null;
        list.add(0);
        Integer p1 = 0;
        list.add(p1);
        Integer p2 = 1;
        list.add(p2);
        Integer p3 = 2;
        list.add(p3);
        Number number = module.callFunction(emulator, 0x28395, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
        return result;
    }


    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "com/twl/signer/YZWG->gContext:Landroid/content/Context;":
                return new StringObject(vm, "AppController");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/lang/String->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            case "android/content/pm/PackageManager->getPackagesForUid(I)[Ljava/lang/String;":
                String[] strArray= {"com.hpbr.bosszhipin"};

                return newStringArray(vm, strArray);

        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {

            case "java/lang/String->hashCode()I": {
                return 0x1C9ECC8;

            }
        }

        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    public void hook_nativeDecodeContent() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0xB3B6 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
                Pointer str_2 = ctx.getPointerArg(1);
//                buffer = ctx.getPointerArg(2);
                System.out.println("参数1：" + str_1.getString(0));
                System.out.println("参数2：" + str_2.getString(0));
//                System.out.println("参数3：" + str_3.getString(0));
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                Inspector.inspect(buffer.getByteArray(0, 0x200), "inline hook EncryptWallEncode");
            }
        });
    }

    public void HookByConsoleDebugger(){
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base+0xB36C+1);

    }

    public static void main(String[] args) throws Exception {
        boss test = new boss();
        test.hook_nativeDecodeContent();
//        test.HookByConsoleDebugger();
        System.out.println(test.callDecodeContent());
    }
}

