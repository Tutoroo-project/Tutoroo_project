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
        // 1. 랭킹 리스트 조회
        const dto = await rankingApi.getRankings(filterGender, filterAge);
        
        // [수정 포인트] response.data 전체가 아니라, 내부의 'allRankers' 배열을 꺼내야 함
        if (dto && dto.allRankers) {
            setRankingList(dto.allRankers);
        } else {
            setRankingList([]);
        }

        // 2. 내 랭킹 조회 (로그인 상태가 아니면 실패할 수 있으므로 분리 처리 권장)
        try {
            const myData = await rankingApi.getMyRanking();
            setMyRanking(myData);
        } catch (e) {
            console.log("비로그인 상태이거나 내 정보 로드 실패");
            setMyRanking(null);
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

          <div css={s.contentRow}>
            {/* rankingList는 이제 확실히 배열임 */}
            <RankingList rankingList={rankingList} isLoading={isLoading} />
            <MyRankingCard myRanking={myRanking} />
          </div>
        </div>
      </div>
    </>
  );
}

export default RankingPage;