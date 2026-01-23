/** @jsxImportSource @emotion/react */
import Header from "../../components/layouts/Header";
import * as s  from "./styles";
function Pet() {
    
    return <>
    <Header />
    <div css={s.wrapper}>
        <div css={s.contentBox}>
            <div css={s.mainContainer}></div>
            <button css={s.btn}></button>
        </div>
    </div>
    </>
}

export default Pet;
