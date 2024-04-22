package project.domain.photo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhotoUrl {

    @Column(name = "photo_url")
    // 원본 사진 url
    private String photoUrl;

    @Column(name = "thumb_url1")
    private String thumb_url1;

    @Column(name = "thumb_url2")
    private String thumb_url2;

    @Column(name = "thumb_url3")
    private String thumb_url3;
}
