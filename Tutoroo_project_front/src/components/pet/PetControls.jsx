import React from 'react';

const PetControls = ({ isSleeping, actions }) => {
    if (isSleeping) {
        return (
            <button onClick={actions.wakeUp} style={styles.wakeBtn}>
                ⏰ 깨우기
            </button>
        );
    }

    return (
        <div style={styles.grid}>
            <button onClick={actions.feed} style={styles.btn}>🍖 밥주기</button>
            <button onClick={actions.play} style={styles.btn}>⚽ 놀아주기</button>
            <button onClick={actions.clean} style={styles.btn}>✨ 씻겨주기</button>
            <button onClick={actions.sleep} style={styles.btn}>💤 재우기</button>
        </div>
    );
};

const styles = {
    wakeBtn: { width: '100%', padding: '15px', background: '#3F51B5', color: 'white', border: 'none', borderRadius: '10px', fontSize: '16px', cursor: 'pointer' },
    grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' },
    btn: { padding: '12px', background: 'white', border: '1px solid #ddd', borderRadius: '8px', fontSize: '14px', cursor: 'pointer', transition: 'background 0.2s' }
};

export default PetControls;