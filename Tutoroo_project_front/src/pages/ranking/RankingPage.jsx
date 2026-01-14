/** @jsxImportSource @emotion/react */
import { useState, useEffect } from "react";
import Header from "../../components/layouts/Header";
import { rankingApi } from "../../apis/ranking/rankingApi";
import * as s from "./styles";

function RankingPage() {
  const [rankingList, setRankingList] = useState([]);
  const [myRanking, setMyRanking] = useState(null);  

  const [filterGender, setFilterGender] = useState("전체");
  const [filterAge, setFilterAge] = useState("전체");
  
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        const listData = await rankingApi.getRankings(filterGender, filterAge);
        setRankingList(listData);
        const myData = await rankingApi.getMyRanking();
        setMyRanking(myData);

      } catch (error) {
        console.error("랭킹 데이터 로드 실패:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [filterGender, filterAge]);

  return (
    <>
      <Header />
      <div css={s.pageBg}>
        <div css={s.container}>
          
          {/* 상단 타이틀 & 필터 */}
          <section css={s.topSection}>
            <h1 css={s.pageTitle}>포인트 월간 랭킹</h1>
            <div css={s.filterWrap}>
              <select 
                css={s.filterSelect} 
                value={filterGender}
                onChange={(e) => setFilterGender(e.target.value)}
              >
                <option value="전체">성별 전체</option>
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
              </select>
              <select 
                css={s.filterSelect}
                value={filterAge}
                onChange={(e) => setFilterAge(e.target.value)}
              >
                <option value="전체">연령 전체</option>
                <option value="10">10대</option>
                <option value="20">20대</option>
                <option value="30">30대</option>
              </select>
            </div>
          </section>

          {/* 메인 컨텐츠 */}
          <div css={s.contentRow}>
            
            {/* [좌측] 랭킹 리스트 */}
            <div css={s.rankListArea}>
              {isLoading ? (
                <div style={{ textAlign: "center", padding: "50px" }}>로딩 중...</div>
              ) : rankingList.length > 0 ? (
                rankingList.map((user, index) => {
                   const rank = user.dailyRank || index + 1; 

                   return (
                    <div key={user.id || index} css={s.rankCard(rank)}>
                      
                      {/* 순위 아이콘/텍스트 */}
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

                      {/* 프로필 & 이름 */}
                      <div css={s.userInfo}>
                        {/* 프로필 이미지가 있으면 보여주고 없으면 기본 아이콘 */}
                        {user.profileImage ? (
                          <img src={user.profileImage} css={s.userProfileImg} alt="profile" />
                        ) : (
                          <div css={s.userIcon} />
                        )}
                        <span css={s.userName}>{user.name || user.username}</span>
                      </div>

                      {/* 포인트 */}
                      <div css={s.pointText}>{user.totalPoint?.toLocaleString()} P</div>
                    </div>
                  );
                })
              ) : (
                <div style={{ textAlign: "center", padding: "50px", color: "#666" }}>
                  랭킹 데이터가 없습니다.
                </div>
              )}
            </div>

            {/* [우측] 내 포인트 현황 카드 */}
            <aside css={s.myStatusArea}>
              <div css={s.statusCard}>
                <h3 css={s.cardTitle}>포인트 현황</h3>
                
                {myRanking ? (
                  <div css={s.cardContent}>
                    <span css={s.cardLabel}>누적 포인트 / 랭킹</span>
                    <div css={s.bigPoint}>
                      {myRanking.totalPoint?.toLocaleString() || 0} P
                    </div>
                    <div css={s.currentRank}>
                      현재 전체 {myRanking.dailyRank || "-"}위
                    </div>
                    <div css={s.userInfo} style={{ marginTop: '20px', justifyContent: 'center' }}>
                         {myRanking.profileImage ? (
                          <img src={myRanking.profileImage} css={s.userProfileImg} alt="my profile" />
                        ) : (
                          <div css={s.userIcon} />
                        )}
                        <span css={s.userName}>{myRanking.name}</span>
                    </div>
                  </div>
                ) : (
                  <div style={{ textAlign: "center", color: "#999", padding: "20px" }}>
                    로그인 정보가<br/>없습니다.
                  </div>
                )}
              </div>
            </aside>

          </div>
        </div>
      </div>
    </>
  );
}

export default RankingPage;