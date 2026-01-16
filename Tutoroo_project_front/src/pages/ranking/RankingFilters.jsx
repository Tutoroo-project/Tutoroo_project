/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function RankingFilters({ filterGender, setFilterGender, filterAge, setFilterAge }) {
  return (
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
          <option value="0">10대 미만</option>
          <option value="10">10대</option>
          <option value="20">20대</option>
          <option value="30">30대</option>
          <option value="40">40대</option>
          <option value="50">50대</option>
          <option value="60">60대 이상</option>
        </select>
      </div>
    </section>
  );
}

export default RankingFilters;