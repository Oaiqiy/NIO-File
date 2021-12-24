package test;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CompareFile {
    public static void main(String[] args) throws Exception {

        //compare("F:\\FromC\\新建文件夹 (2)\\Captures\\腾讯会议 2020-06-02 12-59-19.mp4","F:\\test\\腾讯会议 2020-06-02 12-59-19.mp4");

        System.out.println("source MD5:" + DigestUtils.md5Hex(new FileInputStream("F:\\test\\from0" + "\\腾讯会议 2020-06-10 10-33-48.mp4")));

        for(int i=0;i<16;i++){
            System.out.println("to"+i+"MD5:"+DigestUtils.md5Hex(new FileInputStream("F:\\test\\to"+i+"\\腾讯会议 2020-06-10 10-33-48.mp4")));
        }
    }

    public static boolean compare(String path1,String path2) throws Exception{
        File one = new File(path1);
        File two = new File(path2);

        var oneS = new FileInputStream(one);
        var twoS = new FileInputStream(two);

        var oneMD5 = DigestUtils.md5Hex(oneS);
        var twoMD5 = DigestUtils.md5Hex(twoS);

        System.out.println(oneMD5);
        System.out.println(twoMD5);

        return oneMD5.equals(twoMD5);
    }


}
