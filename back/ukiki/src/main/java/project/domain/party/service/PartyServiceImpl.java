package project.domain.party.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.domain.member.entity.Member;
import project.domain.party.dto.request.EnterPartyDto;
import project.domain.party.dto.response.PartyEnterDto;
import project.domain.party.entity.MemberParty;
import project.domain.member.entity.MemberRole;
import project.domain.member.repository.MemberRepository;
import project.domain.party.dto.request.CreatePartyDto;
import project.domain.party.dto.response.PartyLinkDto;
import project.domain.party.entity.Party;
import project.domain.party.mapper.PartyLinkMapper;
import project.domain.party.repository.MemberpartyRepository;
import project.domain.party.redis.PartyLink;

import project.domain.party.repository.PartyLinkRedisRepository;
import project.domain.party.repository.PartyRepository;
import project.global.exception.BusinessLogicException;
import project.global.exception.ErrorCode;
import project.global.util.BcryptUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@AllArgsConstructor
public class PartyServiceImpl implements PartyService {

    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final MemberpartyRepository memberpartyRepository;
    private final PartyLinkRedisRepository partyLinkRedisRepository;
    private final PartyLinkMapper partyLinkMapper;

    private final BcryptUtil bcryptUtil;

    @Override
    @Transactional
    public PartyLinkDto createParty(CreatePartyDto createPartyDto, MultipartFile photo) {
        // TODO 유저 아이디를 토큰에서 받아야 함
        Member member = memberRepository.findById(1L)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));

        Pattern namePattern = Pattern.compile("^[0-9a-zA-Z가-힣\\\\/!\\\\-_.*'()\\\\s]{1,10}$");    // 따옴표 안에 있는 패턴 추출.
        Matcher matcher = namePattern.matcher(createPartyDto.getPartyName());
        if (!matcher.matches()) {
            throw new BusinessLogicException(ErrorCode.PARTY_NAME_INVALID);
        }

        Pattern passwordPattern = Pattern.compile("^[0-9a-zA-Z\\!@#$%^*+=-]{8,15}$");    // 따옴표 안에 있는 패턴 추출.
        Matcher matcher2 = passwordPattern.matcher(createPartyDto.getPassword());
        if (!matcher2.matches()) {
            throw new BusinessLogicException(ErrorCode.PARTY_PASSWORD_INVALID);
        }

        // TODO 이미지 저장 해야함
        // String partyThumbnail = imageUploader.upload(photo);
        String partyThumbnail = "TEST-ADDRESS";

        Party party = Party.builder()
            .partyName(createPartyDto.getPartyName())
            .thumbnail(partyThumbnail)
            .password(bcryptUtil.encodeBcrypt(createPartyDto.getPassword()))
            .build();
        partyRepository.save(party);

        MemberParty memberParty = MemberParty.builder()
            .memberRole(MemberRole.MASTER)
            .party(party)
            .member(member)
            .build();
        memberpartyRepository.save(memberParty);

//        //TODO Redis에 링크 저장
        String link = makeLink(); // 고유한 link가 나오도록 반복
//        while (partyLinkRedisRepository.findById(link).isPresent()){
//            link = makeLink();
//        }

        PartyLink partyLink = PartyLink.builder()
            .partyLink(link)
            .party(party)
            .build();

//        partyLinkRedisRepository.save(partyLink);

        return partyLinkMapper.toPartyLinkDto(partyLink);
    }

    @Override
    @Transactional
    public PartyLinkDto createLink(Long partyId) {

        // TODO 유저 아이디를 토큰에서 받아야 함
        Member member = memberRepository.findById(1L)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_NOT_FOUND));

        MemberParty memberParty = memberpartyRepository.findByMemberAndParty(member, party)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.FORBIDDEN_ERROR));


        // TODO Redis 연결 안됨
        // 기존 경로는 삭제
        Optional<PartyLink> existLink = partyLinkRedisRepository.findByParty(party);
        existLink.ifPresent(partyLinkRedisRepository::delete);

        String link = makeLink(); // 고유한 link가 나오도록 반복
//        while (partyLinkRedisRepository.findById(link).isPresent()){
//            link = makeLink();
//        }

        PartyLink partyLink = PartyLink.builder()
            .partyLink(link)
            .party(party)
            .build();

//        partyLinkRedisRepository.save(partyLink);

        return partyLinkMapper.toPartyLinkDto(partyLink);
    }

    @Override
    @Transactional(readOnly = true)
    public void enterParty(String link) {
        // redis에 없는 파티 참여 링크라면 에러 반환
        partyLinkRedisRepository.findById(link)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_LINK_INVALID));
    }

    @Override
    @Transactional
    public void checkPassword(EnterPartyDto enterPartyDto) {
        PartyLink partyLink = partyLinkRedisRepository.findById(enterPartyDto.getLink())
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_LINK_INVALID));

        // 비밀번호 비교
        if (!bcryptUtil.matchesBcrypt(enterPartyDto.getPassword(), partyLink.getParty().getPassword())) {
            if (partyLink.getCount() == 1) {   // 카운트를 다 사용했으면 링크 제거
                partyLinkRedisRepository.delete(partyLink);
                throw new BusinessLogicException(ErrorCode.INPUT_NUMBER_EXCEED);
            }
            partyLink.setCount(partyLink.getCount() - 1); // 카운트 -1 하기
            partyLinkRedisRepository.save(partyLink);

            throw new BusinessLogicException(ErrorCode.PARTY_PASSWORD_INVALID);
        }
    }

    @Override
    @Transactional
    public PartyEnterDto memberPartyEnter(EnterPartyDto enterPartyDto) {
        // TODO 유저 아이디를 토큰에서 받아야 함
        Member member = memberRepository.findById(1L)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));

        PartyLink partyLink = partyLinkRedisRepository.findById(enterPartyDto.getLink())
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_LINK_INVALID));

        // 파티에 유저 뷰어 권한으로 넣어주기
        MemberParty memberParty = MemberParty.builder()
            .party(partyLink.getParty())
            .member(member)
            .memberRole(MemberRole.VIEWER)
            .build();

        memberpartyRepository.save(memberParty);

        PartyEnterDto res = partyLinkMapper.toPartyEnterDto(partyLink);
        return res;
    }

    @Override
    public PartyEnterDto guestPartyEnter(EnterPartyDto enterPartyDto) {

        PartyLink partyLink = partyLinkRedisRepository.findById(enterPartyDto.getLink())
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.PARTY_LINK_INVALID));

        //TODO 게스트용 토큰 만들어야함.
        String guestToken = "asd2123_asd1kas1+asd";


        PartyEnterDto res = partyLinkMapper.toPartyEnterDto(partyLink);
        res.setToken(guestToken);
        return res;
    }



    public String makeLink() {

        Map<Integer, List<Integer>> numsRange = new HashMap<>() {{
            put(0, new ArrayList<>(List.of(10, 48)));
            put(1, new ArrayList<>(List.of(26, 65)));
            put(2, new ArrayList<>(List.of(26, 97)));
        }};

        String returnValue = "";
        for (int i = 0; i < 10; i++) {
            int j = (int) (Math.random() * 10) % 3;
            List<Integer> nums = numsRange.get(j);
            String word = String.valueOf((char) (((int) (Math.random() * nums.getFirst()) + nums.getLast())));
            returnValue = returnValue.concat(word);
        }

        return returnValue;
    }
}
