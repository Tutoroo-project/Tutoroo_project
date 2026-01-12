import { css } from "@emotion/react";
import { theme } from "./theme";

export const global = css`
  * {
    box-sizing: border-box;
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

  /* =========================
     🔥 Swiper 강제 복구
     ========================= */

  .swiper-wrapper {
    display: flex !important;
    flex-direction: row !important;
  }

  .swiper-slide {
    flex-shrink: 0;
  }
`;
