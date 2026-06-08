package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.Dict;
import com.example.aimanager.service.DictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/items/{typeCode}")
    public ResponseEntity<?> getItems(@PathVariable String typeCode) {
        return ResponseEntity.ok(Result.success(dictService.getItemsByType(typeCode)));
    }
}
