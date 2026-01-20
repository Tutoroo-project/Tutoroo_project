/** @jsxImportSource @emotion/react */
import { useState, useEffect } from "react";
import Header from "../../components/layouts/Header";
import { rankingApi } from "../../apis/ranking/rankingApi";
import RankingFilters from "./RankingFilters";
import RankingList from "./RankingList";
import MyRankingCard from "./MyRankingCard";
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
        // 1. 전체 랭킹 리스트 호출
        const data = await rankingApi.getRankings(filterGender, filterAge);
        
        if (data) {
          setRankingList(data.allRankers || []);

          // [핵심 로직 수정]
          // Case A: 백엔드에서 내 랭킹 정보를 줬으면 -> 그대로 사용
          if (data.myRank) {
            setMyRanking(data.myRank);
          } 
          // Case B: 랭킹 리스트에 내가 없어서 null로 왔다면 -> 직접 조회 (Fallback)
          else {
            try {
              // 프로필(이미지) + 대시보드(점수) 동시에 호출
              const [profile, dashboard] = await Promise.all([
                rankingApi.getMyProfile(),
                rankingApi.getMyDashboard()
              ]);

              // 두 데이터를 합쳐서 'RankingDTO.RankEntry'와 같은 모양으로 만듦
              setMyRanking({
                rank: dashboard.rank,           // 0이면 '순위 없음' 처리됨
                maskedName: profile.name,       // 이름
                totalPoint: dashboard.currentPoint, // 포인트
                profileImage: profile.profileImage // 이미지
              });
            } catch (e) {
              console.log("로그인 상태가 아니거나 데이터를 불러올 수 없습니다.");
              setMyRanking(null);
            }
          }
        }
      } catch (error) {
        console.error("랭킹 데이터 로드 실패:", error);
        setRankingList([]);
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
          <RankingFilters
            filterGender={filterGender}
            setFilterGender={setFilterGender}
            filterAge={filterAge}
            setFilterAge={setFilterAge}
          />
          
          <div css={s.contentWrap}>
            <RankingList 
              rankingList={rankingList} 
              isLoading={isLoading} 
            />
            
            {/* 데이터가 조합된 myRanking을 전달 */}
            <MyRankingCard myRanking={myRanking} />
          </div>
        </div>
      </div>
    </>
  );
}

export default RankingPage;