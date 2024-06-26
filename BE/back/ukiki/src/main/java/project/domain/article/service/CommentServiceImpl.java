package project.domain.article.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import project.domain.alarm.redis.Alarm;
import project.domain.alarm.redis.AlarmType;
import project.domain.alarm.repository.AlarmRedisRepository;
import project.domain.alarm.service.AlarmService;
import project.domain.article.collection.CommentCollection;
import project.domain.article.dto.request.CommentDto;
import project.domain.article.dto.response.ArticleCreateResDto;
import project.domain.article.entity.Article;
import project.domain.article.repository.ArticleRepository;
import project.domain.article.repository.CommentRepository;
import project.domain.member.dto.request.CustomUserDetails;
import project.domain.member.entity.Member;
import project.domain.member.entity.MemberRole;
import project.domain.member.entity.Profile;
import project.domain.member.repository.MemberRepository;
import project.domain.member.repository.ProfileRepository;
import project.domain.party.repository.MemberpartyRepository;
import project.global.exception.BusinessLogicException;
import project.global.exception.ErrorCode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ArticleRepository articleRepository;
    private final AlarmService alarmService;
    private final AlarmRedisRepository alarmRedisRepository;
    private final MemberpartyRepository memberpartyRepository;
    @Override
    @Transactional
    public void createComment(ArticleCreateResDto articleCreateResDto) {
        Optional<CommentCollection> cc = commentRepository.findById(articleCreateResDto.getArticleId());


        // 이미 있다면 끝.
        if(cc.isPresent()){
            return ;
        }

        // CommentCollection 객체 생성,
        CommentCollection commentCollection = CommentCollection.builder()
                .id(articleCreateResDto.getArticleId())
                .build();

        commentRepository.save(commentCollection);
    }

    @Override
    @Transactional
    public CommentCollection articleComment(Long articleId) {

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }

        return commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));
    }

    @Override
    @Transactional
    public void enterComment(Long articleId, CommentDto commentDto) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }


        // 유정 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));

        // 게시글 정보 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.ARTICLE_NOT_FOUND));

        // 프로필 정보 조회
        Profile profile = profileRepository.findByMemberIdAndPartyId(memberId, article.getParty().getId())
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));

        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));


        // 댓글 객체 생성.
        CommentCollection.Comment newComment = CommentCollection.Comment.builder()
                .userId(memberId)
                .userName(member.getUserName())
                .content(commentDto.getContent())
                .createdDate(myDate())
                .profileUrl(profile.getProfileUrl())
                .build();

        commentDto.getTagList()
                .forEach(collectionTag -> newComment.getTag()
                        .add(new CommentCollection.tag(collectionTag.getUserId(),collectionTag.getUserName())));


        cc.getComment().add(newComment);


        commentRepository.save(cc);

        // 알림 보내기
        Long receiverId = article.getMember().getId();
        Integer commentSize = cc.getComment().size() - 1;
        Alarm alarm = new Alarm(alarmService.createAlarm(AlarmType.COMMENT, article.getParty().getId(), articleId, Long.valueOf(commentSize), memberId, commentDto.getContent()), receiverId);
        System.out.println(alarm.getIdentifier());
        if(!article.getMember().getId().equals(memberId)){
            alarmRedisRepository.save(alarm);
            SseEmitter emitter = alarmService.findEmitterByUserId(receiverId);
            alarmService.sendAlarm(emitter, receiverId, alarm);
        }

        // 태그가 있을 때
        if(!newComment.getTag().isEmpty()){
            for (CommentCollection.tag tag : newComment.getTag()) {
                Long receiverPk = tag.getUserId();
                String tagNick = profile.getNickname();
                memberRepository.findById(receiverPk)
                     .orElseThrow(()-> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));
                memberpartyRepository.findByMemberIdAndPartyId(receiverPk, alarm.getPartyId())
                    .ifPresentOrElse(memberParty -> {
                        if(MemberRole.BLOCK.equals(memberParty.getMemberRole())){
                            throw new BusinessLogicException(ErrorCode.MEMBER_PARTY_NOT_FOUND);
                        }
                    }, ()-> {
                        throw new BusinessLogicException(ErrorCode.INVALID_MEMBER_ROLE);
                    });
                Alarm tagAlarm = new Alarm(alarm, receiverPk);
                tagAlarm.setWriterNick(tagNick);
                tagAlarm.setAlarmType(AlarmType.MENTION);
                alarmRedisRepository.save(tagAlarm);
                SseEmitter tagEmitter = alarmService.findEmitterByUserId(receiverPk);
                alarmService.sendAlarm(tagEmitter, receiverPk, tagAlarm);
            }
        }

    }

    @Override
    @Transactional
    public void modifyComment(Long articleId, Integer commentIdx, CommentDto commentDto) {

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }

        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 체크
        if(cc.getComment().size() <= commentIdx){
            throw new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 작성자 체크
        if(!Objects.equals(cc.getComment().get(Math.toIntExact(commentIdx)).getUserId(), memberId)){
            throw new BusinessLogicException(ErrorCode.USER_NOT_MATCH);
        }

        // 내용 수정
        cc.getComment().get(commentIdx).setContent(commentDto.getContent());
        cc.getComment().get(commentIdx).setCreatedDate(myDate());

        commentRepository.save(cc);
    }

    @Override
    @Transactional
    public void deleteComment(Long articleId, Integer commentIdx) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }


        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 체크
        if(cc.getComment().size() <= commentIdx){
            throw new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 삭제 체크
        cc.getComment().get(commentIdx).setIsDelete(true);

        commentRepository.save(cc);

    }

    @Override
    @Transactional
    public void enterReply(Long articleId, Integer commentIdx, CommentDto commentDto) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }


        // 유정 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));

        // 게시글 정보 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.ARTICLE_NOT_FOUND));

        // 프로필 정보 조회
        Profile profile = profileRepository.findByMemberIdAndPartyId(memberId, article.getParty().getId())
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));

        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 객체 생성.
        CommentCollection.Reply newReply = CommentCollection.Reply.builder()
                .userId(memberId)
                .userName(member.getUserName())
                .createdDate(myDate())
                .content(commentDto.getContent())
                .profileUrl(profile.getProfileUrl())
                .build();

        // 태그 객체 추가
        commentDto.getTagList()
                .forEach(collectionTag -> newReply.getTag()
                        .add(new CommentCollection.tag(collectionTag.getUserId(),collectionTag.getUserName())));

        cc.getComment().get(commentIdx).getReply().add(newReply);

        commentRepository.save(cc);

        Long receiverId = cc.getComment().get(commentIdx).getUserId();
        Alarm alarm = new Alarm(alarmService.createAlarm(AlarmType.REPLY, article.getParty().getId(), articleId, Long.valueOf(commentIdx), memberId, commentDto.getContent()), receiverId);
        if(!receiverId.equals(memberId)){
            alarmRedisRepository.save(alarm);
            SseEmitter emitter = alarmService.findEmitterByUserId(receiverId);
            alarmService.sendAlarm(emitter, receiverId, alarm);
        }

        // 태그가 있을 때
        if(!newReply.getTag().isEmpty()){
            for (CommentCollection.tag tag : newReply.getTag()) {
                Long receiverPk = tag.getUserId();
                String tagNick = profile.getNickname();
                memberRepository.findById(receiverPk)
                    .orElseThrow(()-> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));
                memberpartyRepository.findByMemberIdAndPartyId(receiverPk, alarm.getPartyId())
                    .ifPresentOrElse(memberParty -> {
                        if(MemberRole.BLOCK.equals(memberParty.getMemberRole())){
                            throw new BusinessLogicException(ErrorCode.MEMBER_PARTY_NOT_FOUND);
                        }
                    }, ()-> {
                        throw new BusinessLogicException(ErrorCode.INVALID_MEMBER_ROLE);
                    });
                Alarm tagAlarm = new Alarm(alarm, receiverPk);
                tagAlarm.setWriterNick(tagNick);
                tagAlarm.setAlarmType(AlarmType.MENTION);
                alarmRedisRepository.save(tagAlarm);
                SseEmitter tagEmitter = alarmService.findEmitterByUserId(receiverPk);
                alarmService.sendAlarm(tagEmitter, receiverPk, tagAlarm);
            }
        }

    }

    @Override
    @Transactional
    public void modifyReply(Long articleId, Integer commentIdx, Integer replyIdx, CommentDto commentDto) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }


        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 체크
        if(cc.getComment().size() <= commentIdx
                || cc.getComment().get(commentIdx).getReply().size() <= replyIdx){
            throw new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 작성자 체크
        if(!Objects.equals(cc.getComment().get(commentIdx).getReply().get(replyIdx).getUserId(), memberId)){
            throw new BusinessLogicException(ErrorCode.USER_NOT_MATCH);
        }

        // 내용 수정
        cc.getComment().get(commentIdx).getReply().get(replyIdx).setContent(commentDto.getContent());

        cc.getComment().get(commentIdx).getReply().get(replyIdx).setCreatedDate(myDate());

        commentRepository.save(cc);
    }

    @Override
    @Transactional
    public void deleteReply(Long articleId, Integer commentIdx, Integer replyIdx) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 유저 확인
        if(userDetails == null){
            throw new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Long memberId = userDetails.getId();

        // GUEST 차단
        if(memberId == 0){
            throw new BusinessLogicException(ErrorCode.NOT_ROLE_GUEST);
        }


        CommentCollection cc = commentRepository.findById(articleId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 체크
        if(cc.getComment().size() <= commentIdx
                || cc.getComment().get(commentIdx).getReply().size() <= replyIdx){
            throw new BusinessLogicException(ErrorCode.COMMENT_NOT_FOUND);
        }

        cc.getComment().get(commentIdx).getReply().get(replyIdx).setIsDelete(true);

        commentRepository.save(cc);
    }

    public String myDate(){

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return format.format(date);
    }
}
