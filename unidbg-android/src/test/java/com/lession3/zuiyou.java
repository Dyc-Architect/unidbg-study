package com.lession3;

import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;



public class zuiyou extends AbstractJni{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    zuiyou() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.xiaochuankeji.tieba").build(); // 创建模拟器实例
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\lession3\\right573.apk")); // 创建Android虚拟机
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\lession3\\libnet_crypto.so"), true); // 加载so到虚拟内存
        module = dm.getModule(); //获取本SO模块的句柄

        vm.setJni(this);
        vm.setVerbose(true);
        dm.callJNI_OnLoad(emulator);
    };

    public void native_init() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclass，直接填0，一般用不到。
        module.callFunction(emulator, 0x4a069, list.toArray());
    }

    private String callSign(byte[] bArr) {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(vm.addLocalObject(new StringObject(vm, "https://api.izuiyou.com/index/recommend")));
        ByteArray plaintext = new ByteArray(vm, bArr);
        list.add(vm.addLocalObject(plaintext));
        Number number = module.callFunction(emulator, 0x4a28D, list.toArray())[0];
        return vm.getObject(number.intValue()).getValue().toString();
    }
    private String callEncodeAES() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        String json = new String("{\"filter\": \"all\", \"auto\": 0, \"tab\": \"推荐\", \"direction\": \"down\",\n" +
                "        \"c_types\": [1, 2, 11, 15, 16, 17, 52, 53, 40, 50, 41, 70, 22, 25, 27],\n" +
                "        \"sdk_ver\": {\"tt\": \"3.5.0.8\", \"tx\": \"4.351.1221\", \"mimo\": \"5.0.4\", \"tt_aid\": \"5004095\", \"tx_aid\": \"1106701465\",\n" +
                "                    \"mimo_aid\": \"2882303761517279567\"}, \"ad_wakeup\": 2,\n" +
                "        \"h_ua\": \"Mozilla\\/5.0 (Linux; Android 6.0.1; Nexus 6P Build\\/MMB29M; wv) AppleWebKit\\/537.36 (KHTML, like Gecko) Version\\/4.0 Chrome\\/44.0.2403.117 Mobile Safari\\/537.36\",\n" +
                "        \"manufacturer\": \"Huawei\", \"h_av\": \"5.7.3\", \"h_dt\": 0, \"h_os\": 23, \"h_app\": \"zuiyou\", \"h_model\": \"Nexus 6P\",\n" +
                "        \"h_did\": \"960c0429c3ca048b\", \"h_nt\": 1, \"h_m\": 259693571, \"h_ch\": \"huawei\", \"h_ts\": 1634007434811,\n" +
                "        \"token\": \"T6KfNUd_5QyNu76GVRe6pZS5z535Ruj1xYxrpnxsD2ImkRSnHMQl5hFmCdbAfZxsq29tUxURb0Khi6x2gMTRkudKUIQ==\",\n" +
                "        \"android_id\": \"960c0429c3ca048b\",\n" +
                "        \"h_ids\": {\"imei2\": \"867979020204282\", \"imei1\": \"867979020204282\", \"imei\": \"867979020204282\"}}");
        ByteArray plaintext = new ByteArray(vm, json.getBytes(StandardCharsets.UTF_8));
        list.add(vm.addLocalObject(plaintext));
        Number number = module.callFunction(emulator, 0x4a0b9, list.toArray())[0];
        return vm.getObject(number.intValue()).getValue().toString();
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;":
                return new StringObject(vm, "AppController");
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/String->getPackageManager()Landroid/content/pm/PackageManager;":
                return null;
            case "java/lang/String->getFilesDir()Ljava/io/File;":
            case "java/lang/String->getAbsolutePath()Ljava/lang/String;":
                return new StringObject(vm, "/data/data/com.xiaochuankeji.tieba/files/");
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z":
                return false;
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Process->myPid()I":
                return emulator.getPid();
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }
    
    public void callMd5(){
        List<Object> list = new ArrayList<>(10);

        // arg1
        String input = "r0ysue";
        // malloc memory
        MemoryBlock memoryBlock1 = emulator.getMemory().malloc(16, false);
        // get memory pointer
        UnidbgPointer input_ptr=memoryBlock1.getPointer();
        // write plainText on it
        input_ptr.write(input.getBytes(StandardCharsets.UTF_8));

        // arg2
        int input_length = input.length();

        // arg3 -- buffer
        MemoryBlock memoryBlock2 = emulator.getMemory().malloc(16, false);
        UnidbgPointer output_buffer=memoryBlock2.getPointer();

        // 填入参入
        list.add(input_ptr);
        list.add(input_length);
        list.add(output_buffer);
        // run
        module.callFunction(emulator, 0x65540 + 1, list.toArray());

        // print arg3
        Inspector.inspect(output_buffer.getByteArray(0, 0x10), "output");
    };


    public static void main(String[] args) throws Exception {
        zuiyou test = new zuiyou();
//        test.callMd5();

        test.native_init();
//        ByteArray bytes_ = test.callEncodeAES().getBytes(StandardCharsets.UTF_8);
//        System.out.println(test.callSign(test.callEncodeAES()));
        System.out.println(test.callEncodeAES());
    }
}

