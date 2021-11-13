package com.sougou;

// 导入通用且标准的类库

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.hook.IHook;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// 继承AbstractJni类
public class sougou extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    sougou() {
        // 创建模拟器实例,进程名建议依照实际进程名填写，可以规避针对进程名的校验
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.sina.oasis").build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\sougou\\sougou.apk"));
        //
//        vm = emulator.createDalvikVM(null);

        // 加载目标SO
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\sougou\\libSCoreTools.so"), true); // 加载so到虚拟内存
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        vm.setJni(this); // 设置JNI
        vm.setVerbose(true); // 打印日志
        emulator.traceWrite(module.base+0x3A0C0, module.base+0x3A0C0);
        dm.callJNI_OnLoad(emulator); // 调用JNI OnLoad
    }

    ;

    public void native_init() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        list.add(vm.addLocalObject(context));
        module.callFunction(emulator, 0x9565, list.toArray());

    }

    public void encryptDemo() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        String str = "http://app.weixin.sogou.com/api/searchapp";
        String str2 = "type=2&ie=utf8&page=1&query=%E5%A5%8B%E9%A3%9E%E5%AE%89%E5%85%A8&select_count=1&tsn=1&usip=";
        String str3 = "lilac";
        list.add(vm.addLocalObject(new StringObject(vm, str)));
        list.add(vm.addLocalObject(new StringObject(vm, str2)));
        list.add(vm.addLocalObject(new StringObject(vm, str3)));
        Number number = module.callFunction(emulator, 0x9ca1, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
        System.out.println(result);
    }

    // HookZz hook Sc_EncryptWallEncode
    public void hookEncryptWallEncode() {
        // 获取HookZz对象
        IHookZz hookZz = HookZz.getInstance(emulator); // 加载HookZz，支持inline hook，
        //文档看https://github.com/jmpews/HookZz
        // enable hook
        hookZz.enable_arm_arm64_b_branch(); // 测试enable_arm_arm64_b_branch
        hookZz.wrap(module.base + 0xA284 + 1, new
                WrapCallback<HookZzArm32RegisterContext>() {
                    Pointer buffer;

                    @Override
                    // 方法执行前
                    public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext
                            ctx, HookEntryInfo info) {
                        System.out.println("HookZz hook EncryptWallEncode");
                        //再看一下Unidbg完整的代码
                        Pointer input1 = ctx.getPointerArg(0);
                        Pointer input2 = ctx.getPointerArg(1);
                        Pointer input3 = ctx.getPointerArg(2);
                        // getString的参数i代表index,即input[i:]
                        System.out.println("参数1：" + input1.getString(0));
                        System.out.println("参数2：" + input2.getString(0));
                        System.out.println("参数3：" + input3.getString(0));
                        buffer = ctx.getPointerArg(3);
                    }

                    ;

                    @Override
                    // 方法执行后
                    public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext
                            ctx, HookEntryInfo info) {
                        // getByteArray参数1是起始index，参数2是长度，我们不知道结果多长，就先设置0x100吧
                        byte[] outputhex = buffer.getByteArray(0, 0x100);
                        Inspector.inspect(outputhex, "EncryptWallEncode output");
                    }
                });
        hookZz.disable_arm_arm64_b_branch();
    }

    public void hookencryptwall_wallkey_wallkey() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0xB134 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                byte[] outputhex = buffer.getByteArray(0, 0x100);
                Inspector.inspect(outputhex, "encryptwall_wallkey_wallkey output");
            }
        });
    }
//    public void inlinehookEncryptWallEncode() {
//        IHookZz hookZz = HookZz.getInstance(emulator);
//        hookZz.enable_arm_arm64_b_branch();
//        hookZz.instrument(module.base + 0x9d24 + 1, new
//                InstrumentCallback<Arm32RegisterContext>() {
//                    Pointer buffer;
//                    @Override
//                    public void dbiCall(Emulator<?> emulator, Arm32RegisterContext ctx,
//                                        HookEntryInfo info) {
//                        System.out.println("HookZz inline hook EncryptWallEncode");
//                        Pointer input1 = ctx.getPointerArg(0);
//                        Pointer input2 = ctx.getPointerArg(1);
//                        Pointer input3 = ctx.getPointerArg(2);
//                        // getString的参数i代表index,即input[i:]
//                        System.out.println("参数1：" + input1.getString(0));
//                        System.out.println("参数2：" + input2.getString(0));
//                        System.out.println("参数3：" + input3.getString(0));
//                        buffer = ctx.getPointerArg(3);
//                    }
//                });
//        hookZz.instrument(module.base + 0x9d28 + 1, new
//                InstrumentCallback<Arm32RegisterContext>() {
//                    Pointer buffer;
//                    @Override
//                    public void dbiCall(Emulator<?> emulator, Arm32RegisterContext ctx,
//                                        HookEntryInfo info) {
//                        buffer = ctx.getPointerArg(0);
//                        Inspector.inspect(buffer.getByteArray(0, 0x100), "inline hook EncryptWallEncode");
//                    }
//                });
//    }

//    public void HookByConsoleDebugger(){
//        Debugger debugger = emulator.attach();
////        debugger.addBreakPoint(module.base+0x9d24);
////        debugger.addBreakPoint(module.base+0x9d28);
////        debugger.addBreakPoint(module.base+0xC504);
////        debugger.addBreakPoint(module.base+0xC504);
//    }
    public void hook_rsa_encrypt() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();
        hookZz.wrap(module.base + 0xC504 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer input1 = ctx.getPointerArg(0);
                Pointer input2 = ctx.getPointerArg(1);
                Pointer input3 = ctx.getPointerArg(2);
                Pointer input4 = ctx.getPointerArg(3);
                byte[] input1_byte = input1.getByteArray(0, 0x100);
                Inspector.inspect(input1_byte, "rsa_encrypt input1");
//                byte[] input2_byte = input2.getByteArray(0, 0x100);
//                System.out.println(input2.getString(0) + "rsa_encrypt input2");

            }
            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                byte[] outputhex = buffer.getByteArray(0, 0x100);
                Inspector.inspect(outputhex, "rsa_encrypt output");
            }
        });
    }

    public void hook_RSA_eay_public_encrypt() {
        IHookZz hookzz = HookZz.getInstance(emulator);
        hookzz.enable_arm_arm64_b_branch();
        hookzz.wrap(module.base + 0x141C0 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer input1 = ctx.getPointerArg(0);
                Pointer input2 = ctx.getPointerArg(1);
                Pointer input3 = ctx.getPointerArg(2);
                Pointer input4 = ctx.getPointerArg(3);
                System.out.println("hook_RSA_eay_public_encrypt");
//                byte[] input1_byte = input4.getByteArray(0, 0x100);
//                Inspector.inspect(input1_byte, "RSA_eay_public_encrypt input1");
//                byte[] input2_byte = input2.getByteArray(0, 0x100);
//                Inspector.inspect(input2_byte, "PublicEnc input2");
                byte[] input3_byte = input3.getByteArray(0, 0x100);
                Inspector.inspect(input3_byte, "PublicEnc input3");
                buffer = ctx.getPointerArg(2);
//                byte[] input4_byte = input4.getByteArray(0, 0x100);
//                Inspector.inspect(input4_byte, "RSA_eay_public_encrypt input4");
//                buffer = ctx.getPointerArg(4);
            }
            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
//                buffer = ctx.getPointerArg(0);
                byte[] outputhex = buffer.getByteArray(0, 0x100);
                Inspector.inspect(outputhex, "RSA_eay_public_encrypt output");
            }
        });
    }

    public void hook_base64() {
        IHookZz hookzz = HookZz.getInstance(emulator);
        hookzz.enable_arm_arm64_b_branch();
        hookzz.wrap(module.base + 0x11046 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer input1 = ctx.getPointerArg(0);
                Pointer input2 = ctx.getPointerArg(1);
                Pointer input3 = ctx.getPointerArg(2);
                Pointer input4 = ctx.getPointerArg(3);
                byte[] input1_byte = input1.getByteArray(0, 0x100);
                Inspector.inspect(input1_byte, "base64 input1");
//                byte[] input2_byte = input2.getByteArray(0, 0x100);
//                Inspector.inspect(input2_byte, "PublicEnc input2");
//                byte[] input3_byte = input3.getByteArray(0, 0x100);
//                Inspector.inspect(input3_byte, "PublicEnc input3");
//                byte[] input4_byte = input4.getByteArray(0, 0x100);
//                Inspector.inspect(input4_byte, "PublicEnc input4");

            }
            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                byte[] outputhex = buffer.getByteArray(0, 0x100);
                Inspector.inspect(outputhex, "base64 output");
            }
        });
    }

    public static void main(String[] args) {
        sougou test = new sougou();
//        test.hookEncryptWallEncode();
//        test.inlinehookEncryptWallEncode();
//        test.HookByConsoleDebugger();
        test.hookEncryptWallEncode();
        test.hookencryptwall_wallkey_wallkey();
        test.hook_rsa_encrypt();
        test.hook_RSA_eay_public_encrypt();
        test.hook_base64();
        test.native_init();
        test.encryptDemo();
    }
}

