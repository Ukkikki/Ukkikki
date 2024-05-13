package project.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ChatPageDto {

    int size;
    int page;

    boolean next;
    List<SimpleChatDto> simpleChatDtos;
}
