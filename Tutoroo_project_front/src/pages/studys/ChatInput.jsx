/** @jsxImportSource @emotion/react */
import { useRef } from "react";
import * as s from "./styles";
import { HiMiniSpeakerWave, HiMiniSpeakerXMark } from "react-icons/hi2";
import { FaCircle } from "react-icons/fa";
import { PiMicrophoneStageFill } from "react-icons/pi";
import { MdImage, MdClose } from "react-icons/md";

const ChatInput = ({ 
  inputText, setInputText, handleSend, handleKeyDown,
  isSpeakerOn, toggleSpeaker, isRecording, startRecording, stopRecording,
  chatImageFile, setChatImageFile, chatImagePreview, setChatImagePreview,
  isChatLoading, currentMode, handleDownloadPdf 
}) => {
  const fileInputRef = useRef(null);

  return (
    <>
      <div css={s.controlToolbar}>
        <button css={s.iconBtn(isSpeakerOn)} onClick={toggleSpeaker}>
          {isSpeakerOn ? <HiMiniSpeakerWave /> : <HiMiniSpeakerXMark />}
        </button>
        <button 
          css={s.iconBtn(isRecording)} 
          onMouseDown={startRecording} onMouseUp={stopRecording}
          onTouchStart={startRecording} onTouchEnd={stopRecording}
        >
          {isRecording ? <FaCircle /> : <PiMicrophoneStageFill />}
        </button>
        <button 
          css={s.iconBtn(!!chatImageFile)} 
          onClick={() => fileInputRef.current?.click()}
        >
          <MdImage />
        </button>
        <input
          type="file" ref={fileInputRef} hidden accept="image/*"
          onChange={(e) => setChatImageFile(e.target.files?.[0])}
        />
        {currentMode === 'REVIEW' && (
          <button css={s.textBtn} onClick={handleDownloadPdf}>자료 다운</button>
        )}
      </div>
      
      {chatImagePreview && (
        <div css={s.imagePreviewContainer}>
          <img src={chatImagePreview} alt="preview" css={s.imagePreview} />
          <button 
            css={s.removeImageBtn}
            onClick={() => { setChatImageFile(null); setChatImagePreview(null); }}
          >
            <MdClose />
          </button>
        </div>
      )}
      
      <div css={s.inputWrapper}>
        <input 
          type="text" 
          placeholder={isRecording ? "듣고 있습니다..." : "질문해보세요."}
          css={s.inputBox}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={isChatLoading || isRecording}
          autoFocus
        />
      </div>
      <button css={s.sendBtn} onClick={handleSend} disabled={isChatLoading || isRecording}>
        전송
      </button>
    </>
  );
};

export default ChatInput;