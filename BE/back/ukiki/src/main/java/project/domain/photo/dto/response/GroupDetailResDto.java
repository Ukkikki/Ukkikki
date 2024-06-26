package project.domain.photo.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupDetailResDto {

    private String fileId;
    private Long photoId;
    private String photoUrl;
    private String thumbnailUrl;
}
