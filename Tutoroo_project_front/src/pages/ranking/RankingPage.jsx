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

          // Case A: 서버가 내 랭킹 정보를 줬을 때 (순위권 내)
          if (data.myRank) {
            setMyRanking(data.myRank);
          } 
          // Case B: 순위권 밖이라서 내 정보가 없을 때 (직접 조회)
          else {
            try {
              const [profile, dashboard] = await Promise.all([
                rankingApi.getMyProfile(),
                rankingApi.getMyDashboard()
              ]);
              
              // [중요] 이름 마스킹 처리 (김철수 -> 김*수)
              let masked = profile.name;
              if (masked && masked.length >= 2) {
                masked = masked.charAt(0) + "*" + masked.substring(2);
              }

              // MyRankingCard가 기대하는 필드명(maskedName)으로 데이터 구성
              setMyRanking({
                rank: dashboard.rank,               
                maskedName: masked,   
                name: profile.name,   
                totalPoint: dashboard.currentPoint, 
                profileImage: profile.profileImage  
              });
            } catch (e) {
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
            <MyRankingCard myRanking={myRanking} />
          </div>
        </div>
      </div>
    </>
  );
}

export default RankingPage;