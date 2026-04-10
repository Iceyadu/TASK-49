package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.QuestionCreateRequest;
import com.scholarops.model.entity.KnowledgeTag;
import com.scholarops.model.entity.Question;
import com.scholarops.model.entity.QuestionBank;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping("/question-banks")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<List<QuestionBank>>> listBanks(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(questionBankService.getBanks(currentUser.getId())));
    }

    @PostMapping("/question-banks")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<QuestionBank>> createBank(@RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        QuestionBank bank = questionBankService.createBank(
                body.get("name"), body.get("description"), body.get("subject"), currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(bank));
    }

    @GetMapping("/question-banks/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<QuestionBank>> getBank(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(questionBankService.getBank(id, currentUser.getId())));
    }

    @PostMapping("/question-banks/{id}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<Question>> addQuestion(@PathVariable Long id,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        request.setQuestionBankId(id);
        Question question = questionBankService.addQuestion(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(question));
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<Question>> updateQuestion(@PathVariable Long id,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.updateQuestion(id, request, currentUser.getId())));
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        questionBankService.deleteQuestion(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/knowledge-tags")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<List<KnowledgeTag>>> listTags() {
        return ResponseEntity.ok(ApiResponse.success(questionBankService.getAllTags()));
    }

    @PostMapping("/knowledge-tags")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUESTION_BANK_MANAGE')")
    public ResponseEntity<ApiResponse<KnowledgeTag>> createTag(@RequestBody Map<String, String> body) {
        KnowledgeTag tag = questionBankService.createTag(body.get("name"), body.get("category"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(tag));
    }
}
