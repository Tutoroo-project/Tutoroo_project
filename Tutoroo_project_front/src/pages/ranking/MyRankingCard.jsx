/** @jsxImportSource @emotion/react */
import * as s from "./styles";

import defaultProfileImg from "../../assets/images/mascots/default_image.png";

function MyRankingCard({ myRanking }) {
  return (
    <aside css={s.myStatusArea}>
      <div css={s.statusCard}>
        <h3 css={s.cardTitle}>내 랭킹 현황</h3>

        {myRanking ? (
          <div css={s.cardContent}>
            
            <div css={s.userInfo} style={{ marginLeft: 0, flexDirection: 'column' }}>
              
              {/* 회색 배경 프레임 */}
              <div 
                css={s.profileFrame} 
                style={{ width: '80px', height: '80px', border: '4px solid #fff' }}
              >
                <img 
                  src={myRanking.profileImage || defaultProfileImg}
                  css={s.profileImgContent} 
                  alt="My Profile" 
                  
                  onError={(e) => {
                    e.target.src = defaultProfileImg;
                  }}
                />
              </div>

              <span css={s.userName} style={{ fontSize: '20px', marginTop: '10px' }}>
                {myRanking.maskedName || myRanking.name}
              </span>
            </div>

            <span css={s.cardLabel} style={{ marginTop: '10px' }}>
              현재 순위 및 포인트
            </span>

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
            랭킹 정보를 불러올 수 없습니다.
          </div>
        )}
      </div>
    </aside>
  );
}

export default MyRankingCard;