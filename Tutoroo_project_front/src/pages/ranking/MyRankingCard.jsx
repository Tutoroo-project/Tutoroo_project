/** @jsxImportSource @emotion/react */
import * as s from "./styles";
import defaultProfileImg from "../../assets/images/mascots/default_image.png";

function MyRankingCard({ myRanking }) {
  
  const getImageUrl = (url) => {
    if (!url) {
      return defaultProfileImg;
    }
    if (url.startsWith("http")) {
      return url;
    }
    const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const cleanBase = rawBaseUrl.replace(/\/$/, ""); 
    const cleanPath = url.startsWith("/") ? url : `/${url}`;

    return `${cleanBase}${cleanPath}`;
  };

  const finalImageUrl = getImageUrl(myRanking?.profileImage);
  return (
    <aside css={s.myStatusArea}>
      <div css={s.statusCard}>
        <h3 css={s.cardTitle}>내 랭킹 현황</h3>

        {myRanking ? (
          <div css={s.cardContent}>
            <div css={s.myUserInfo}>              
              <div css={s.myProfileImg(finalImageUrl)} />
              <span css={s.userName}>
                {myRanking.maskedName || myRanking.name}
              </span>
            </div>
            <span css={s.cardLabel}>
              현재 순위 / 포인트 
            </span>

            <div css={s.bigPoint}>
              <span className="rank">
                {myRanking.rank && myRanking.rank > 0 
                  ? `${myRanking.rank}위` 
                  : '순위 없음'}
              </span>
              |
              <span className="point-value">
                {myRanking.totalPoint?.toLocaleString() || 0} P
              </span>
            </div>
            
          </div>
        ) : (
          <div css={s.isUnauthenticated}>
            로그인이 필요하거나<br />
            정보를 불러올 수 없습니다.
          </div>
        )}
      </div>
    </aside>
  );
}

export default MyRankingCard;