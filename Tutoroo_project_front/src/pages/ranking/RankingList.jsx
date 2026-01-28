/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import defaultProfileImg from "../../assets/images/mascots/default_image.png";

function RankingList({ rankingList, isLoading }) {

  const getImageUrl = (url) => {
    if (!url) return defaultProfileImg;
    if (url.startsWith("http")) return url;
    
    const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const cleanBase = rawBaseUrl.replace(/\/$/, "");
    const cleanPath = url.startsWith("/") ? url : `/${url}`;

    return `${cleanBase}${cleanPath}`;
  };

  return (
    <div css={s.rankListArea}>
      {isLoading ? (
        <div css={s.loadingText}>랭킹 데이터를 불러오는 중...</div>
      ) : rankingList && rankingList.length > 0 ? (
        rankingList.map((user, index) => {
          const rank = user.rank; 
          
          const finalImageUrl = getImageUrl(user.profileImage);

          return (
            <div key={index} css={s.rankCard(rank)}>
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
                <div css={s.userProfileImg(finalImageUrl)} />
                
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