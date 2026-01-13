/** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import SessionStatus from "../../components/studys/SessionStatus";
import useStudyStore from "../../stores/useStudyStore";
import * as s from "./styles";

function StudyPage() {
  const { selectedTutorId } = useStudyStore();

  return (
    <>
      <Header />
      <div css={s.pageContainer}>
        {/* 채팅 영역 */}
        <main css={s.chatArea}>
            <div css={s.placeholder}>
                <p><strong>{selectedTutorId}</strong> 선생님과의 수업이 시작되었습니다.</p>
                <p style={{fontSize: 14}}>AI가 먼저 말을 걸어올 것입니다...</p>
            </div>
        </main>

        {/* 하단 입력 영역 */}
        <footer css={s.bottomArea}>
            <div css={s.bottomInner}>
                {/* [왼쪽 하단 위젯] */}
                <SessionStatus />

                {/* 채팅 입력창 */}
                <div css={s.inputWrapper}>
                    <input type="text" placeholder="답변을 입력하세요." css={s.inputBox} />
                </div>
                
                <button css={s.sendBtn}>전송</button>
            </div>
        </footer>
      </div>
    </>
  );
}

export default StudyPage;