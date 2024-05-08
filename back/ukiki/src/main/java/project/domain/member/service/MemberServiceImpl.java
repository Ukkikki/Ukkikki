package project.domain.member.service;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import project.domain.member.dto.request.CustomUserDetails;
import project.domain.member.dto.response.InfoDto;
import project.domain.member.entity.Member;
import project.domain.member.redis.MemberToken;
import project.domain.member.repository.MemberRepository;
import project.domain.member.repository.MemberTokenRedisRepository;
import project.global.exception.BusinessLogicException;
import project.global.exception.ErrorCode;
import project.global.jwt.JWTUtil;

import java.io.PrintWriter;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final MemberTokenRedisRepository memberTokenRedisRepository;
    private final JWTUtil jwtUtil;

    /*
    내 정보를 처음 가져올 때 사용할 함수.
     */
    @Override
    public InfoDto myInfo() {

        // 로그인 성공했을때 저장해두었던 값을들 가져온다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        InfoDto infoDto = null;

        // 널 값 체크
        if(authentication != null){
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            long userId = userDetails.getId();

            Optional<Member> optionalMember = memberRepository.findById(userId);
            // 널 값 체크
            if(optionalMember.isPresent()){
                // 불러온 값들을 Dto에 넣어 리턴한다.
                Member member = optionalMember.get();

                infoDto = new InfoDto();
                infoDto.setProfileUrl(member.getProfileUrl());
                infoDto.setUserName(member.getUserName());
                infoDto.setUserId(member.getId());

            }
        }

        return infoDto;
    }

    @Override
    public String reissue(Cookie[] cookies) {

        String refresh = null;

        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals("refresh")){
                    refresh = cookie.getValue();
                }
            }

        }

        // 토큰이 없어요.
        if(refresh == null){
            throw new BusinessLogicException(ErrorCode.REFRESH_TOKEN_NULL);
        }

        // refresh토큰 체크
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            throw new BusinessLogicException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }


        String reUsername = jwtUtil.getUsername(refresh);
        String reProviderId = jwtUtil.getProviderId(refresh);
        Long id = jwtUtil.getId(refresh);

        MemberToken memberToken = memberTokenRedisRepository.findById(id)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.REFRESH_TOKEN_NULL));

        // 토큰이 일치하지 않아요.
        if(!refresh.equals(memberToken.getToken())){
            throw new BusinessLogicException(ErrorCode.REFRESH_TOKEN_MATCH);
        }

        refresh = jwtUtil.createJWT("access", id, reUsername, reProviderId, ((1000L * 60) * 60 * 4));

        return refresh;
    }
}
