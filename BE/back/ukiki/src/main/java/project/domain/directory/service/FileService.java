package project.domain.directory.service;

import java.util.List;
import project.domain.directory.collection.File;
import project.domain.directory.collection.Trash;
import project.domain.directory.dto.response.GetDetailFileDto;
import project.domain.directory.dto.response.GetDirDto;
import project.domain.photo.entity.Photo;

public interface FileService {

    // file생성
    String createFile(Long partyId, Photo photo);

    // 사진 복사
    void copyFile(String fileId, String fromDirId, String toDirId);

    // 복수 사진 복사
    void copyFileList(List<String> fileIdList, String fromDirId, String toDirId);

    // 사진 이동
    void moveFile(String fileId, String fromDirId, String toDirId);

    // 복수 사진 이동
    void moveFileList(List<String> fileIdList, String fromDirId, String toDirId);

    // 단일 사진 삭제
    void deleteOneFile(String fileId, String dirId, String sseKey);

    void deleteFileList(List<String> fileIdList, String dirId, String sseKey);

    public Trash saveFileToTrash(File file, String dirId);

    // dir, file의 관계 설정 메서드
    void setDirFileRelation(String dirId, String fileId);

    // dir, file의 관계 삭제 메서드
    void deleteDirFileRelation(String dirId, String fileId);

    String generateId();

    File findById(String fileId);

    GetDetailFileDto getFile(String fileId);
}
