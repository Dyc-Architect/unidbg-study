
function monitor_constants(targetSo) {
    let const_array = [];
    let const_name = [];
    let const_addr = [['MD5 K table', '0x65bc'], ['MD5 K table', '0x6cd8'], ['SHA1 / SHA0 / RIPEMD-160 Initial Constants value', '0x9420'], ['SHA1 / SHA0 / RIPEMD-160 Initial Constants value', '0x9498'], ['SHA224 Initial Constants value', '0x14d88'], ['SHA256 Initial Constants value', '0x14da8'], ['SHA256 / SHA224 K tabke', '0x14dc8'], ['SHA384 Initial Constants value', '0x15148'], ['SHA512 Initial Constants value', '0x153c8'], ['SHA512 / SHA384 K table', '0x14ec8']];

    for (var i = 0; i < const_addr.length; i++) {
        const_array.push({base:targetSo.add(const_addr[i][1]),size:0x1});
        const_name.push(const_addr[i][0]);
    }

    MemoryAccessMonitor.enable(const_array, {
        onAccess: function (details) {
            console.log("\n");
            console.log("监控到疑似加密常量的内存访问\n");
            console.log(const_name[details.rangeIndex]);
            console.log("访问来自:"+details.from.sub(targetSo)+"(可能有误差)");
    }
});
}

function hook_suspected_function(targetSo) {
    const funcs = [['tjcreate', '函数tjcreate疑似哈希函数，包含初始化魔数的代码。', '0x2c49'], ['sub_53C8', '函数sub_53C8疑似哈希函数运算部分。', '0x53c9'], ['sub_58E4', '函数sub_58E4疑似哈希函数运算部分。', '0x58e5'], ['sub_6188', '函数sub_6188疑似哈希函数运算部分。', '0x6189'], ['sub_689C', '函数sub_689C疑似哈希函数运算部分。', '0x689d'], ['sub_735C', '函数sub_735C疑似哈希函数运算部分。', '0x735d'], ['sub_8374', '函数sub_8374疑似哈希函数运算部分。', '0x8375'], ['sub_951C', '函数sub_951C疑似哈希函数运算部分。', '0x951d'], ['sub_9FBC', '函数sub_9FBC疑似哈希函数运算部分。', '0x9fbd'], ['CC_DES_encrypt1', '函数CC_DES_encrypt1疑似哈希函数运算部分。', '0xc31d']];
    for (var i in funcs) {
        let relativePtr = funcs[i][2];
        let funcPtr = targetSo.add(relativePtr);
        let describe = funcs[i][1];
        let handler = (function() {
        return function(args) {
            console.log("\n");
            console.log(describe);
            console.log(Thread.backtrace(this.context,Backtracer.ACCURATE).map(DebugSymbol.fromAddress).join("\n"));
        };
        })();
    Interceptor.attach(funcPtr, {onEnter: handler});
}
}


function main() {
    var targetSo = Module.findBaseAddress('libtujia_encrypt.so');
    // 对疑似哈希算法常量的地址进行监控，使用frida MemoryAccessMonitor API，有几个缺陷，在这里比较鸡肋。
    // 1.只监控第一次访问，所以如果此区域被多次访问，后续访问无法获取。可以根据这篇文章做改良和扩展。https://bbs.pediy.com/thread-262104-1.htm
    // 2.ARM 64无法使用
    // 3.无法查看调用栈
    // 在这儿用于验证这些常量是否被访问，访问了就说明可能使用该哈希算法。
    // MemoryAccessMonitor在别处可能有较大用处，比如ollvm过的so，或者ida xref失效/过多等情况。
    // hook和monitor这两个函数，只能分别注入和测试，两个同时会出错，这可能涉及到frida inline hook的原理
    // 除非hook_suspected_function 没结果，否则不建议使用monitor_constants。
    // monitor_constants(targetSo);

    hook_suspected_function(targetSo);
}

setImmediate(main);
    