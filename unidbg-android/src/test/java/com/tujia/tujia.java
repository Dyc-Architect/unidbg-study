package com.tujia;

// 导入通用且标准的类库
import com.github.unidbg.Emulator;
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
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 继承AbstractJni类
public class tujia extends AbstractJni{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    tujia() {
        // 创建模拟器实例,进程名建议依照实际进程名填写，可以规避针对进程名的校验
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.sina.oasis").build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\tujia\\tujia.apk"));
        //
//        vm = emulator.createDalvikVM(null);

        // 加载目标SO
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\tujia\\libtujia_encrypt.so"), true); // 加载so到虚拟内存
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        vm.setJni(this); // 设置JNI
        vm.setVerbose(true); // 打印日志

        dm.callJNI_OnLoad(emulator); // 调用JNI OnLoad
    };

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/tujia/hotel/TuJiaApplication->getInstance()Lcom/tujia/hotel/TuJiaApplication;":
                return vm.resolveClass("com/tujia/hotel/TuJiaApplication").newObject(null);
            case "java/security/MessageDigest->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;":
                StringObject type = varArg.getObjectArg(0);
                String name = "";
                if ("\"SHA1\"".equals(type.toString())) {
                    name = "SHA1";
                } else {
                    name = type.toString();
                    System.out.println("else name: " + name);
                }
                try {
                    return vm.resolveClass("java/security/MessageDigest").newObject(MessageDigest.getInstance(name));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }


        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "com/tujia/hotel/TuJiaApplication->getPackageName()Ljava/lang/String;":
                return new StringObject(vm, "com.tujia.hotel");
            case "com/tujia/hotel/TuJiaApplication->getPackageManager()Landroid/content/pm/PackageManager;":
                DvmClass clazz = vm.resolveClass("android/content/pm/PackageManager");
                return clazz.newObject(signature);
            case "java/security/MessageDigest->digest([B)[B":
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                byte [] array = (byte[]) varArg.getObjectArg(0).getValue();
                byte [] output = messageDigest.digest(array);
                return new ByteArray(vm, output);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    public void call_encrypt() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        Object arg0 = vm.addLocalObject(new StringObject(vm, ""));
        String arg1_str = "Mozilla/5.0 (Linux; Android 8.1.0; OPPO R11st Build/OPM1.171019.011; wv)";
        Object arg1 = vm.addLocalObject(new StringObject(vm, arg1_str));
        String arg2_str = "LON=null;LAT=null;CID=226809753;LAC=42272;";
        Object arg2 = vm.addLocalObject(new StringObject(vm, arg2_str));
        String arg3_str ="{\"code\":null,\"parameter\":{\"abTests\":{\"T_login_831\":{\"s\":true,\"v\":\"A\"},\"searchhuojia\":{\"s\":true,\"v\":\"D\"},\"listfilter_227\":{\"s\":true,\"v\":\"D\"},\"T_renshu_292\":{\"s\":true,\"v\":\"D\"},\"Tlisttest_45664\":{\"s\":true,\"v\":\"C\"},\"Tlist_168\":{\"s\":true,\"v\":\"C\"},\"T_LIST27620\":{\"s\":true,\"v\":\"C\"}}},\"client\":{\"abTest\":{},\"abTests\":{},\"adTest\":{\"m1\":\"Color OS V5.2.1\",\"m2\":\"\"}";
        Object arg3 = vm.addLocalObject(new StringObject(vm, arg3_str));
        int arg4 = arg3_str.getBytes(StandardCharsets.UTF_8).length;
        long arg5 = 1630845461L;
        list.add(arg0);
        list.add(arg1);
        list.add(arg2);
        list.add(arg3);
        list.add(arg4);
        list.add(arg5);
        Number number = module.callFunction(emulator, 0x36a9, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
        System.out.println("result: " + result);
    }

    public void get_bodyencrypt() throws FileNotFoundException {
        List<Object> list = new ArrayList<>();

        String arg0_str = "1";
        Object arg0 = vm.addLocalObject(new StringObject(vm, arg0_str));
        long arg1 = 1630808396L;
        String arg2_str = "YM0A2TMAIEWA3xMAMM1xTjQEUYGD0wZAYAh32AdgQATDzAZNZA33WAEIZAzzhAMMNAzyTDAkZMzTizYOYN11TTgMRcDThyNMZO44GTIAVQGDi5OONN2wWGMMUVWj1iMNOYxxGmUU";
        Object arg2 = vm.addLocalObject(new StringObject(vm, arg2_str));
        int arg3 = arg2_str.getBytes(StandardCharsets.UTF_8).length;
        String arg4_str = "{\"code\":null,\"parameter\":{\"activityTask\":{},\"defa";//"{\"code\":null,\"parameter\":{\"activityTask\":{},\"defaultKeyword\":\"\",\"abTest\":{\"AppSearchHouseList\":\"B\",\"listabtest8\":\"B\"},\"returnNavigations\":true,\"returnFilterConditions\":true,\"specialKeyType\":0,\"returnGeoConditions\":true,\"abTests\":{\"T_login_831\":{\"s\":true,\"v\":\"A\"},\"searchhuojiatujia\":{\"s\":true,\"v\":\"D\"},\"listfilter_227\":{\"s\":true,\"v\":\"D\"},\"T_renshu_292\":{\"s\":true,\"v\":\"D\"},\"Tlisttest_45664\":{\"s\":true,\"v\":\"C\"},\"Tlist_168\":{\"s\":true,\"v\":\"C\"},\"T_LIST27620\":{\"s\":false,\"v\":\"A\"}},\"pageSize\":10,\"excludeUnitIdSet\":null,\"historyConditions\":[],\"searchKeyword\":\"\",\"url\":\"\",\"isDirectSearch\":false,\"sceneCondition\":null,\"returnAllConditions\":true,\"searchId\":null,\"pageIndex\":0,\"onlyReturnTotalCount\":false,\"conditions\":[{\"type\":2,\"value\":\"2021-09-05\"},{\"type\":3,\"value\":\"2021-09-06\"},{\"label\":\"大理州\",\"type\":1,\"value\":\"36\"}]},\"client\":{\"abTest\":{},\"abTests\":{},\"adTest\":{\"m1\":\"Color OS V5.2.1\",\"m2\":\"ade40419318f085ff21b4776f2eef21f\",\"m3\":\"armeabi-v7a\",\"m4\":\"armeabi\",\"m5\":\"100\",\"m6\":\"2\",\"m7\":\"5\"},\"api_level\":260,\"appFP\":\"qA/Ch2zqjORBz90YV34sUZpcFXFV6vzhmAISdTjYAFeqMBTtMUukzQFXqkDokr+sMau0bWClwjtk36nbrVBWVrjmrPTCkXFIraNHdgRVW/QT6g4eLWuM3hhP8qsWgGnrErk2KA+GFxr/OBRMYfV4l0v+TYUDZ5k4bUCUawafdLY5b3aC02SuOrqjW3jjrXiB/dt6ErjrDv44vY4Y8/1r5Z6ut/2BmcErxM37MniKpW6EZc8F4CjJ9S1KRTtEPJ2Kkd2Sd8602jqdgtssJ6QKXyx2+qsKvybydVe+zSTXQGn/T86A6uW0oC+mJHwOLnP8HKN0q2Fu3rTcKZ+Prbs/dcBHaWJi1C1tHZFza2O+1gUQTgvg+Kq57BvE6IjEhveT\",\"appId\":\"com.tujia.hotel\",\"appVersion\":\"260_260\",\"appVersionUpdate\":\"rtag-20210803-183436-zhengyuan\",\"batteryStatus\":\"full\",\"buildTag\":\"rtag-20210803-183436-zhengyuan\",\"buildVersion\":\"8.38.0\",\"ccid\":\"51742042410923060391\",\"channelCode\":\"qq\",\"crnVersion\":\"254\",\"devModel\":\"OPPO R11st\",\"devToken\":\"\",\"devType\":2,\"dtt\":\"\",\"electricity\":\"100\",\"flutterPkgId\":\"277\",\"gps\":null,\"kaTest\":{\"k1\":\"2_1_2\",\"k2\":\"sdm660\",\"k3\":\"ubuntu-16\",\"k4\":\"R11st_11_A.43_200402\",\"k5\":\"OPPO/R11st/R11s:8.1.0/OPM1.171019.011/1577198226:user/release-keys\",\"k6\":\"R11st\",\"k7\":\"OPM1.171019.011\"},\"latitude\":\"23.105714\",\"locale\":\"zh-CN\",\"longitude\":\"113.470271\",\"networkType\":\"1\",\"osVersion\":\"8.1.0\",\"platform\":\"1\",\"salt\":\"ZMmADTBAMIzAzxMAYMkxWjVEFY2TkwNMZAh2WAdAFATzmAZMYA3wTAEckAzT2AMMMAz52DAIZMzThzYMMN1xWTgYMcDD0yNNMO41zTIQUQGDx5OOZN2wDGMMEVWjziMNZYxxDmUA\",\"screenInfo\":\"\",\"sessionId\":\"a3e57f1c-03a8-3bc4-8709-fb36e7698ae4_1630844995950\",\"tId\":\"21090509553913313246\",\"tbTest\":{\"j1\":\"11d890e2\",\"j2\":\"R11s\",\"j3\":\"OPPO R11st\",\"j4\":\"OPPO\",\"j5\":\"OPPO\",\"j6\":\"unknown\",\"j7\":\"qcom\",\"j8\":\"2.1.0  (ART)\"},\"traceid\":\"1630845462612_1630845462444_1630845358749\",\"uID\":\"a3e57f1c-03a8-3bc4-8709-fb36e7698ae4\",\"version\":\"260\",\"wifi\":null,\"wifimac\":\"i5ZQ9aI14FDr9VMJ/ECIg7KAOE+Xev1/CrFoa53WLbE=\"},\"psid\":\"76938405-a463-464a-9c49-752022daf516\",\"type\":null,\"user\":null,\"usid\":null}";
        Object arg4 = vm.addLocalObject(new StringObject(vm, arg4_str));
        int arg5 = arg4_str.getBytes(StandardCharsets.UTF_8).length;

        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(arg0);
        list.add(arg1);
        list.add(arg2);
        list.add(arg3);
        list.add(arg4);
        list.add(arg5);
        Number number = module.callFunction(emulator, 0x380c+1, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
        System.out.println("result: " + result);

    }


    public void hook_tj_crypt() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x2E94 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
//                Pointer str_3 = ctx.getPointerArg(2);
                Pointer str_4 = ctx.getPointerArg(3);
                Pointer str_5 = ctx.getPointerArg(4);
                Pointer str_6 = ctx.getPointerArg(5);
                Pointer str_7 = ctx.getPointerArg(6);
                Pointer str_8 = ctx.getPointerArg(7);
                Pointer str_9 = ctx.getPointerArg(8);
                try {
                    System.out.println("hook_tj_crypt 参数一 ： " + str_1.getString(0));;
                }catch (Exception e) {
                    System.out.println("参数一出错");
                }
                try {
                    System.out.println("hook_tj_crypt 参数二 ： " + ctx.getIntArg(1));
                }catch (Exception e) {
                    System.out.println(e + "参数二出错");
                }
                try {
                    System.out.println("参数3");
                    System.out.println(ctx.getR2Long());
                }catch (Exception e) {
                    System.out.println("参数3出错");
                }
                try {
                    System.out.println("参数4");
                    System.out.println(ctx.getR3Long());;
                }catch (Exception e) {
                    System.out.println("参数4出错");
                }
                try {
                    Inspector.inspect(str_5.getByteArray(0, 0x200), "hookzz tj_crypt_str_5");
                }catch (Exception e) {
                    System.out.println("参数5出错");
                }
                try {
                    System.out.println("参数6");
                    System.out.println(ctx.getIntArg(0));;
                }catch (Exception e) {
                    System.out.println("参数6出错");
                }
                try {
                    Inspector.inspect(str_7.getByteArray(0, 0x200), "hookzz tj_crypt_str_7");
                }catch (Exception e) {
                    System.out.println("参数7出错");
                }
                try {
                    System.out.println("参数8");
                    System.out.println(ctx.getIntArg(0));;
                }catch (Exception e) {
                    System.out.println("参数8出错");
                }
                try {
                    Inspector.inspect(str_9.getByteArray(0, 0x200), "hookzz tj_crypt_str_9");
                }catch (Exception e) {
                    System.out.println("参数9出错");
                }

            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz tj_crypt output");
            }
        });
    }

    public void hook_tjtxtutf8() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x291C + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
                Pointer str_2 = ctx.getPointerArg(2);
                System.out.println("str1----->" +str_1.toString());
                Inspector.inspect(str_1.getByteArray(0, 0x200), "hookzz tjtxtutf8");
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz tjtxtutf8 output");
            }
        });
    }


    public void hook_CCCrypt() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x4480 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                System.out.println("hook_CCCrypt参数0 ： " + ctx.getIntArg(0));
                System.out.println("hook_CCCrypt参数1 ： " + ctx.getIntArg(1));
                System.out.println("hook_CCCrypt参数2 ： " + ctx.getIntArg(2));
                Inspector.inspect(ctx.getPointerArg(3).getByteArray(0, 0x200), "hookzz CCCrypt 参数3");
                System.out.println("hook_CCCrypt参数4 ： " + ctx.getIntArg(4));
                System.out.println("hook_CCCrypt参数5 ： " + ctx.getIntArg(5));
                Inspector.inspect(ctx.getPointerArg(6).getByteArray(0, 0x200), "hookzz CCCrypt 参数6");
                System.out.println("hook_CCCrypt参数7 ： " + ctx.getR7Long());
                ctx.push(ctx.getPointerArg(8));
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.pop();
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz CCCrypt output");
            }
        });
    }

    public void hook_sub_302C() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x302C + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
                Pointer str_2 = ctx.getPointerArg(1);
                Pointer str_3 = ctx.getPointerArg(2);
                Pointer str_4 = ctx.getPointerArg(3);
//                Inspector.inspect(str_4.getByteArray(0, 0x200), "hookzz sub_302C_str_4");
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.getPointerArg(0);
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz sub_302C output");
            }
        });
    }
    public void hook_CCHmac() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x47B4 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
                Pointer str_2 = ctx.getPointerArg(5);
                Pointer str_3 = ctx.getPointerArg(2);
                Pointer str_4 = ctx.getPointerArg(3);
                Inspector.inspect(str_4.getByteArray(0, 0x200), "hookzz CCHmac_str_4");
                ctx.push(str_2);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.pop();
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz CCHmac output");
            }
        });
    }

    public void hook_sub_33E4() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x33E4 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer str_1 = ctx.getPointerArg(0);
                Pointer str_2 = ctx.getPointerArg(5);
                Pointer str_3 = ctx.getPointerArg(2);
                Pointer str_4 = ctx.getPointerArg(3);
                Inspector.inspect(str_1.getByteArray(0, 0x200), "hookzz  sub_33E4_str_1");
                ctx.push(str_1);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.pop();
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz  sub_33E4 output");
            }
        });
    }

    public void hook_CCCryptorCreate() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0x33E4 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Inspector.inspect(ctx.getPointerArg(0).getByteArray(0, 0x100), "hook_CCCryptorCreate 参数0");
                System.out.println("hook_CCCryptorCreate 参数1 ：" + ctx.getR1Int());
//                Inspector.inspect(ctx.getPointerArg(2).getByteArray(0, 0x100), "hook_CCCryptorCreate 参数2");
//                Inspector.inspect(ctx.getPointerArg(3).getByteArray(0, 0x100), "hook_CCCryptorCreate 参数3");
//                System.out.println("hook_CCCryptorCreate参数4 ： " + ctx.getIntArg(4));
//                System.out.println("hook_CCCryptorCreate参数5 ： " + ctx.getIntArg(5));
//                Inspector.inspect(ctx.getPointerArg(6).getByteArray(0, 0x100), "hook_CCCryptorCreate 参数6");
                ctx.push(ctx.getPointerArg(18));
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.pop();
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz  CCCryptorCreate output");
            }
        });
    }

    public void hook_CC_RC4_set_key() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();;
        hookZz.wrap(module.base + 0xC244 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            Pointer buffer;
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                System.out.println("hook_CC_RC4_set_key 参数1 ：" + ctx.getR1Long());
                Inspector.inspect(ctx.getPointerArg(2).getByteArray(0, 0x200), "hookzz  hook_CC_RC4_set_key output");
                ctx.push(ctx.getPointerArg(0));
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                buffer = ctx.pop();
                Inspector.inspect(buffer.getByteArray(0, 0x200), "hookzz  hook_CC_RC4_set_key output");
            }
        });
    }

    public void HookByConsoleDebugger(){
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base+0x4e89+1);

    }

    public static void main(String[] args)  throws FileNotFoundException {
        tujia test = new tujia();
//        test.hook_tjreset();
//        test.HookByConsoleDebugger();
//        test.hook_tj_crypt();
//        test.hook_sub_302C();
//        test.hook_CCHmac();
//        test.hook_sub_33E4();
//        test.hook_CCCrypt();
//        test.hook_CCCryptorCreate();
//        test.hook_tjtxtutf8();
//        test.call_encrypt();
        test.hook_CC_RC4_set_key();
        test.get_bodyencrypt();
    }
}

