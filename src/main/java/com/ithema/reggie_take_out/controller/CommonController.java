package com.ithema.reggie_take_out.controller;


import com.ithema.reggie_take_out.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {
    @Value("${reggie.path}")
    private String basepath;

    /**
     * 文件上传
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestPart("file") MultipartFile file) throws IOException {  //加上注解@RequestPart("file")可以自定义名称，否则就只能定义为file
        //MultipartFile[]可以实现批量上传
        log.info(file.toString());
        //将文件转存到指定位置，默认是放在c盘的临时目录下

        //保存到文件服务器，oss服务器
        //原始文件名，但是不建议这样做，因为如果有重名的话，会产生文件覆盖，可以使用uuid重命名文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));  //截取原文件名的后缀.jpg

        String fileName = UUID.randomUUID().toString() + suffix;

        //创建一个目录对象
        File dir = new File(basepath);

        if(!dir.exists()){
            //目录不存在，就创建一个目录
            dir.mkdirs(); //mkdirs()可以建立多级文件夹， mkdir()只会建立一级的文件夹，
        }

        file.transferTo(new File(basepath + fileName)); //指定文件上传路径
//        if(photos.length > 0){
//            for (MultipartFile photo : photos) {
//                if(!photo.isEmpty()){
//                    String originalFilename = photo.getOriginalFilename();
//                    photo.transferTo(new File(basepath + originalFilename));
//                }
//            }
//        }
        return R.success(fileName);

    }

    /**
     * 文件下载，上面的upload文件上传成功后，会response一个data，这个response.data就是上面upload方法返回的文件名
     * 这里的name自动接收传过来的response.data，由前端写好了
     * @param response
     * @param name
     * @throws IOException
     */
    @GetMapping("/download")
    public void download(HttpServletResponse response, String name) throws IOException {
        //输入流，通过输入流读取文件中的内容
        FileInputStream fileInputStream = new FileInputStream(new File(basepath + name));

        //输出流，通过输出流将文件写回浏览器，在浏览器展示图片
        ServletOutputStream outputStream = response.getOutputStream();

        //设置内容的类型
        response.setContentType("image/jpeg");

        int len = 0;
        byte[] bytes = new byte[1024];
        while((len = fileInputStream.read(bytes)) != -1){
            outputStream.write(bytes,0,len);
            outputStream.flush();
        }

        outputStream.close();
        fileInputStream.close();

    }
}
