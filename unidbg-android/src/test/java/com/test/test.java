package com.test;


public class test
{
    public static void main(String[] args)
    {
        //Original String
        String string = "hello world";

        //Convert to byte[]
        byte[] zhanghao = {78, 106, 73, 49, 79, 122, 65, 51, 89, 71, 65, 117, 78, 106, 78, 109, 78, 122, 99, 55, 89, 109, 85, 61};
        byte[] mima = {89, 87, 66, 108, 79, 109, 90, 110, 78, 106, 65, 117, 79, 109, 74, 109, 78, 122, 65, 120, 79, 50, 89, 61};
        //Convert back to String
        String zhanghao_ = new String(zhanghao);
        String mima_ = new String(mima);

        //Check converted string against original String
        System.out.println("Decoded zhanghao_ : " + zhanghao_);
        System.out.println("Decoded mima_ : " + mima_);
    }
}
