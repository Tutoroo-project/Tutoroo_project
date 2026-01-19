import { css } from "@emotion/react";
import { theme } from "./theme";

export const global = css`
html, body {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
    scrollbar-gutter: stable;
  }

  /* 2. 모든 태그 초기화 (마진, 패딩, 박스 크기 계산) */
  * {
    margin: 0;
    padding: 0; /* 패딩도 0으로 초기화해야 미세한 들뜸 방지 */
    box-sizing: border-box; /* 테두리, 패딩을 너비에 포함 */
  }

  body {
    margin: 0;
    background-color: ${theme.colors.pageBg};
    font-family: ${theme.typography.fontFamily};
  }

  button {
    border: none;
    cursor: pointer;
    font-family: inherit;
  }
`;
