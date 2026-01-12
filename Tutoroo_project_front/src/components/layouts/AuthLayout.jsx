/** @jsxImportSource @emotion/react */

import * as s from "./styles";

function AuthLayout({ children }) {
    return (
        <div css={s.authWrapper}>
            <section css={s.brandSection}>
                <img css={s.mascotImg} />
            </section>

            <section css={s.formSection}>
                {children}
            </section>
        </div>
    );
}

export default AuthLayout;
