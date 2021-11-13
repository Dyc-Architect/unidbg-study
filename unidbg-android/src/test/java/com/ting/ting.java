package com.ting;

import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.Emulator;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.Enumeration;
import com.github.unidbg.linux.android.dvm.api.*;
import com.github.unidbg.linux.android.dvm.api.ClassLoader;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.IO;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;


public class ting extends AbstractJni{
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    ting() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.xiaochuankeji.tieba").build(); // 创建模拟器实例
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\ting\\TingTingFM_V5.3.0.apk")); // 创建Android虚拟机
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\ting\\libsecret-generate-native-lib.so"), true); // 加载so到虚拟内存
        module = dm.getModule(); //获取本SO模块的句柄

        vm.setJni(this);
        vm.setVerbose(true);
        dm.callJNI_OnLoad(emulator);
    }

    public void Sign() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        Map<String, String> input = new HashMap<>();
        //  {timestamp=1636683105, session_key=, id=3, channel=34, client=android_2120b2b04360b203b78dff66dce15821, support_styles=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23, version=android_5.3.0}
        //  type: REQUEST
        //        b44f31a63aa8a62a85b5ca7255668779
//        channel=34&client=android_2120b2b04360b203b78dff66dce15821&id=3&session_key=&support_styles=1%2C2%2C3%2C4%2C5%2C6%2C7%2C8%2C9%2C10%2C11%2C12%2C13%2C14%2C15%2C16%2C17%2C18%2C19%2C20%2C21%2C22%2C23&timestamp=1636683105&version=android_5.3.0_yo2xoC60RMTikb7K
        input.put("timestamp", "1636683105");
        input.put("session_key", "");
        input.put("id", "3");
        input.put("channel", "34");
        input.put("client", "android_2120b2b04360b203b78dff66dce15821");
        input.put("support_styles", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23");
        input.put("version", "android_5.3.0");
        list.add(vm.addLocalObject(vm.resolveClass("java/util/Map").newObject(input)));
        list.add(vm.addLocalObject(new StringObject(vm, "REQUEST")));
        Number number = module.callFunction(emulator, 0xEEC4+1, list.toArray())[0];
        System.out.println(vm.getObject(number.intValue()).getValue().toString());

    }

    public void HookByConsoleDebugger(){
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base+0xE398);

    }
    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/util/Map->keySet()Ljava/util/Set;":
                Map<?, ?> map = (Map<?, ?>) dvmObject.getValue();
                return vm.resolveClass("java/util/Set").newObject(map.keySet());
            case "java/util/Set->toArray()[Ljava/lang/Object;":
                Set<?> set = (Set<?>) dvmObject.getValue();
                Object[] array = set.toArray();
                DvmObject[] objects = new DvmObject[array.length];
                for (int i = 0; i < array.length; i++) {
                    if (array[i] instanceof String) {objects[i] = new StringObject(vm, (String) array[i]);
                    }
                    else {throw new IllegalStateException("array=" + array[i]);
                    }
                }
                return new ArrayObject(objects);
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;":
                String str = "";
                if (vaList.getObjectArg(0).getValue().equals("session_key")) {
                    str = "";
                }
                if (vaList.getObjectArg(0).getValue().equals("channel")) {
                    str = "34";
                }
                if (vaList.getObjectArg(0).getValue().equals("client")) {
                    str = "android_2120b2b04360b203b78dff66dce15821";
                }
                if (vaList.getObjectArg(0).getValue().equals("id")) {
                    str = "3";
                }
                if (vaList.getObjectArg(0).getValue().equals("version")) {
                    str = "android_5.3.0";
                }
                if (vaList.getObjectArg(0).getValue().equals("support_styles")) {
                    str = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23";
                }
                if (vaList.getObjectArg(0).getValue().equals("timestamp")) {

                    str = "1636683105";
                }
                return new StringObject(vm, str);

        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/String->getValue()I":
                return 0;
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    public static void main(String[] args) throws Exception {
        ting test = new ting();
//        test.HookByConsoleDebugger();
        test.Sign();
    }


}