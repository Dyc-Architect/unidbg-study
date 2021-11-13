package com.maoyan;

import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.PackageInfo;
import com.github.unidbg.linux.android.dvm.api.Signature;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class maoyan extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;


    maoyan(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.meituan").build();
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(new File("unidbg-android\\src\\test\\java\\com\\maoyan\\110_62cdaa7ac506051d5d6a5a7662847822.apk")); // 创建Android虚拟机
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android\\src\\test\\java\\com\\maoyan\\libmtguard.so"), true);

        module = dm.getModule(); //

        vm.setJni(this);
        dm.callJNI_OnLoad(emulator);
    }

    public static void main(String[] args) {
        maoyan test = new maoyan();
    }

    public String main203(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        list.add(203);
        StringObject input2_1 = new StringObject(vm, "9b69f861-e054-4bc4-9daf-d36ae205ed3e");
        ByteArray input2_2 = new ByteArray(vm, "GET /aggroup/homepage/display __r0ysue".getBytes(StandardCharsets.UTF_8));
        DvmInteger input2_3 = DvmInteger.valueOf(vm, 2);
        vm.addLocalObject(input2_1);
        vm.addLocalObject(input2_2);
        vm.addLocalObject(input2_3);
        // 完整的参数2
        list.add(vm.addLocalObject(new ArrayObject(input2_1, input2_2, input2_3)));
        Number number = module.callFunction(emulator, 0x5a38d, list.toArray())[0];
        return vm.getObject(number.intValue()).getValue().toString();
    };
}

