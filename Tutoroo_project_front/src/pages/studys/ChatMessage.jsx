/** @jsxImportSource @emotion/react */
import * as s from "./styles";

const ChatMessage = ({ msg, isUser, currentTutorImage, getImageSource, onOptionClick }) => {
  const imgSrc = getImageSource(msg.imageUrl);

  return (
    <div css={s.messageRow(isUser)}>
      {!isUser && (
        <div css={s.aiProfileIcon}>
          <img src={currentTutorImage} alt="tutor" />
        </div>
      )} 
      <div css={s.bubble(isUser)}>
        {imgSrc && (
          <img 
            src={imgSrc} 
            alt="session-visual" 
            onError={(e) => { e.target.style.display = 'none'; }} 
          />
        )}
        {msg.hasImage && <span css={s.imageAttachedBadge}>📷 이미지 첨부됨</span>}
        {msg.content}
        
        {msg.testData?.options && (
          <div css={s.testOptions}>
            {msg.testData.options.map((option, idx) => (
              <button
                key={idx}
                css={s.optionButton}
                onClick={() => onOptionClick(option)}
              >
                {idx + 1}. {option}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatMessage;