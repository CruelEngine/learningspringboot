

package com.cruelengine.learningspringboot.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.cruelengine.learningspringboot.datamodels.Image;
import com.cruelengine.learningspringboot.repository.ImageRepository;

@Service
public class ImageService{

    private static String UPLOAD_ROOT = "upload-dir";

    private final ResourceLoader resourceLoader;

    private final ImageRepository imageRepository;
 
    public ImageService(ResourceLoader resourceLoader , ImageRepository imageRepository){
        this.resourceLoader = resourceLoader;
        this.imageRepository = imageRepository;
    }

    @Bean
    CommandLineRunner setUp() throws IOException{
        return (args) -> {
            FileSystemUtils.deleteRecursively(new File(UPLOAD_ROOT));
            Files.createDirectories(Paths.get(UPLOAD_ROOT));

            FileCopyUtils.copy("Test File", new FileWriter(UPLOAD_ROOT + "/learning-spring-boot-cover.jpg"));
            FileCopyUtils.copy("Test File2" , new FileWriter(UPLOAD_ROOT + "/learning-spring-boot-2nd-edition.jpg"));
            FileCopyUtils.copy("Test File3" , new FileWriter(UPLOAD_ROOT + "/bazinga.jpg"));
        };
    }

    public Flux<Image> findAllImages(){
        try{
            return Flux.fromIterable(
                Files.newDirectoryStream(Paths.get(UPLOAD_ROOT)))
                .map(path -> new Image(String.valueOf(path.hashCode()),path.getFileName().toString()));
        }
        catch(IOException e){
                return Flux.empty();
        }
    }

    public Mono<Resource> findOneImage(String fileName){
        return Mono.fromSupplier(()-> 
            resourceLoader.getResource("files:" + UPLOAD_ROOT + "/" + fileName));
    }

    public Mono<Void> createImage(Flux<FilePart> files){

        return files.log("createImage-Files").flatMap(file ->{
            Mono<Image> saveDatabaseImage = imageRepository.save(
                new Image(UUID.randomUUID().toString(), file.filename())).log("createImage-save");
                Mono<Void> copyFile = Mono.just(
                    Paths.get(UPLOAD_ROOT , file.filename()).toFile()).log("createImage-picktarget").map(destFile -> {
                      try{
                          destFile.createNewFile();
                          return destFile;
                      }catch(IOException e){
                          throw new RuntimeException(e);
                      }  
                    }).log("createImage-newfile").flatMap(file::transferTo).log("createImage-copy");
                    return Mono.when(saveDatabaseImage , copyFile).log("createImage-copy"); 
        }).log("createImage-flatMap").then().log("createImage-done");
    }
    
    public Mono<Void> deleteImage(String fileName){
        Mono<Void> deleteDatabaseImage = imageRepository.
        findByName(fileName).
        log("deleteImage-find")
        .flatMap(imageRepository::delete)
        .log("deleteImage-record");

        Mono<Object> deleteFile = Mono.fromRunnable(() -> {
            try{
                Files.deleteIfExists(Paths.get(UPLOAD_ROOT , fileName));
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }).log("deleteImage-File");

        return Mono.when(deleteDatabaseImage , deleteFile).log("deleteImage-When").then().log("deleteImage-done");
    } 
}