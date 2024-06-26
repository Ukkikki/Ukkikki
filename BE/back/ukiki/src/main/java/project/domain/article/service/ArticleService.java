package project.domain.article.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import project.domain.article.dto.request.ArticleCreateDto;
import project.domain.article.dto.request.ArticleUpdateDto;
import project.domain.article.dto.response.ArticleCreateResDto;
import project.domain.article.dto.response.ArticlePageDto;
import project.domain.article.dto.response.SimpleArticleDto;

import java.util.List;

public interface ArticleService {

    ArticleCreateResDto createArticle(Long partyId, ArticleCreateDto articleCreateDto, List<MultipartFile> multipartFiles);
    SimpleArticleDto getArticleDetail(Long partyId, Long articleId);
    ArticlePageDto getArticleList(Long partyId, Pageable pageable);
    SimpleArticleDto updateArticle(Long partyId, Long articleId, ArticleUpdateDto articleUpdateDto, List<MultipartFile> multipartFiles);
    void deleteArticle(Long partyId, Long articleId);
}
