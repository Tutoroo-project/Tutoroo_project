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
          
          {/* 상단 필터 섹션 */}
          <RankingFilters
            filterGender={filterGender}
            setFilterGender={setFilterGender}
            filterAge={filterAge}
            setFilterAge={setFilterAge}
          />

          {/* 메인 컨텐츠 영역 */}
          <div css={s.contentRow}>
            {/* 좌측 랭킹 리스트 */}
            <RankingList rankingList={rankingList} isLoading={isLoading} />

            {/* 우측 내 정보 카드 */}
            <MyRankingCard myRanking={myRanking} />
          </div>
          
        </div>
      </div>
    </>
  );
}

export default RankingPage;