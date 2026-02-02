/** @jsxImportSource @emotion/react */
import { css } from "@emotion/react";

function SpriteChar({src, index = 0, size = 200}) {

    const cols = 4;
    // const safeIndex = index || 0;

    const colIndex = index % cols; //가로(나머지)
    const rowIndex = Math.floor(index / cols); //세로 (몫)

    const xPos = colIndex * 33.3333;
    const yPos = rowIndex * 33.3333;

    const paddingValue = size * 0.05;

    const spriteStyle = css`
        width:${size}px;
        height: ${size}px;
        background-image: url(${src});
        background-size: 400% 400%;
        background-position: ${xPos}% ${yPos}%;
        image-rendering: pixelated; 
        image-rendering: crisp-edges;

        background-repeat: no-repeat;
        background-origin: content-box; 
        padding: ${paddingValue}px;
        box-sizing: border-box; 
        
    `;
    return <div css={spriteStyle} aria-label={`Character frame ${index}`} />
}

export default SpriteChar;