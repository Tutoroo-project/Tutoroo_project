/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function MyRankingCard({ myRanking }) {
  // 요청하신 기본 이미지 경로 상수
  const defaultProfileImg = "/assets/images/mascots/default_image.png";

  return (
    <aside css={s.myStatusArea}>
      <div css={s.statusCard}>
        <h3 css={s.cardTitle}>내 랭킹 현황</h3>

        {myRanking ? (
          <div css={s.cardContent}>
            
            {/* 프로필 이미지 & 이름 영역 */}
            <div css={s.userInfo} style={{ marginLeft: 0, flexDirection: 'column' }}>
              
              <img 
                /* 이미지가 있으면 그걸 쓰고, 없으면 기본 이미지 사용 */
                src={myRanking.profileImage || defaultProfileImg}
                css={s.userProfileImg} 
                style={{ width: '80px', height: '80px' }} 
                alt="My Profile" 
                onError={(e) => {
                  /* 이미지 로드 실패(엑박) 시 기본 이미지로 교체 */
                  e.target.src = defaultProfileImg;
                }}
              />

              {/* 이름 표시 */}
              <span css={s.userName} style={{ fontSize: '20px', marginTop: '10px' }}>
                {myRanking.maskedName}
              </span>
            </div>

            <span css={s.cardLabel} style={{ marginTop: '10px' }}>
              현재 순위 및 포인트
            </span>

            {/* 순위와 포인트 표시 */}
            <div css={s.bigPoint}>
              <span style={{ fontSize: '0.6em', color: '#666', marginRight: '4px' }}>
                {myRanking.rank && myRanking.rank > 0 
                  ? `${myRanking.rank}위` 
                  : '순위 없음'}
              </span>
              |
              <span style={{ marginLeft: '8px' }}>
                {myRanking.totalPoint?.toLocaleString() || 0} P
              </span>
            </div>
            
          </div>
        ) : (
          <div css={s.isUnauthenticated}>
            로그인이 필요하거나<br />
            정보를 불러올 수 없습니다.<br />
            (포인트를 획득해보세요!)
          </div>
        )}
      </div>
    </aside>
  );
}

export default MyRankingCard;