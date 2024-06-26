package project.domain.directory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetDirThumbUrl2 {
    private String pk;
    private String thumbUrl2;
    private Long photoId;
}
