package com.cruelengine.learningspringboot.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.cruelengine.learningspringboot.datamodels.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

@RestController
public class DataControllers{

    private static final Logger log = LoggerFactory.getLogger(DataControllers.class);
    private static final String API_BASE_PATH = "/api";

    @GetMapping(API_BASE_PATH + "/images")
    public Flux<Image> images(){
        return Flux.just(
            new Image("1","learning-spring-boot-cover.jpg"),
            new Image("2","learning-spring-boot-2nd-edition-cover.jpg"),
            new Image("3","bazinga.jpg")
        );
    }

    @PostMapping(API_BASE_PATH + "/images")
    public Mono<Void> create(@RequestBody Flux<Image> images){
        return images.map(image ->{
            log.info("We will save " + image + " to a Reactive database Soon !");
            return image;
        }).then();
    }



}