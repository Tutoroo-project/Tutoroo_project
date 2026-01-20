/** @jsxImportSource @emotion/react */
import * as s from "./styles";

import defaultProfileImg from "../../assets/images/mascots/default_image.png";

function RankingList({ rankingList, isLoading }) {
  return (
    <div css={s.rankListArea}>
      {isLoading ? (
        <div css={s.loadingText}>랭킹 데이터를 불러오는 중...</div>
      ) : rankingList && rankingList.length > 0 ? (
        rankingList.map((user, index) => {
          const rank = user.rank; 

          return (
            <div key={index} css={s.rankCard(rank)}>
              {/* 순위 배지 */}
              <div css={s.rankBadge(rank)}>
                {rank <= 3 ? (
                  <>
                    <span className="medal-icon">
                      {rank === 1 && "🥇"}
                      {rank === 2 && "🥈"}
                      {rank === 3 && "🥉"}
                    </span>
                    {rank}위
                  </>
                ) : (
                  <>{rank}위</>
                )}
              </div>

              <div css={s.userInfo}>
                {/* 회색 배경 프레임 적용 */}
                <div css={s.profileFrame}>
                  <img 
                    // 프사 없으면 기본 이미지
                    src={user.profileImage || defaultProfileImg} 
                    css={s.profileImgContent}
                    alt="profile"
                    
                    // 이미지 로드 에러 시 기본 이미지로 대체
                    onError={(e) => {
                      e.target.src = defaultProfileImg;
                    }}
                  />
                </div>
                
                <span css={s.userName}>{user.maskedName}</span>
              </div>

              <div css={s.pointText}>{user.totalPoint?.toLocaleString()} P</div>
            </div>
          );
        })
      ) : (
        <div css={s.loadingText}>랭킹 데이터가 없습니다.</div>
      )}
    </div>
  );
}

export default RankingList;