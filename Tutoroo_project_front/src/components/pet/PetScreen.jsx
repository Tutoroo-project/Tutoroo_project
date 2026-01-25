import React from 'react';
import PetControls from './PetControls'; 

const PetScreen = ({ pet, actions }) => {
    if (!pet) return null;

    // 펫 이미지 결정
    const getPetImage = () => {
        if (pet.customImageUrl) return pet.customImageUrl;
        // 임시 이미지 URL (실제로는 assets 폴더 이미지 사용)
        return `https://via.placeholder.com/250?text=${pet.petType}_Lv${pet.stage}`;
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h2 style={styles.name}>{pet.petName} <span style={styles.badge}>Lv.{pet.stage}</span></h2>
                <p style={styles.msg}>{pet.statusMessage}</p>
            </div>

            <div style={styles.scene}>
                {pet.isSleeping && <div style={styles.zzz}>ZZZ...</div>}
                <img 
                    src={getPetImage()} 
                    alt={pet.petName}
                    style={{
                        ...styles.img,
                        filter: pet.isSleeping ? 'brightness(0.6) grayscale(50%)' : 'none'
                    }}
                />
            </div>

            <div style={styles.stats}>
                <StatBar label="배고픔" value={pet.fullness} color="#FF9800" />
                <StatBar label="친밀도" value={pet.intimacy} color="#E91E63" />
                <StatBar label="청결도" value={pet.cleanliness} color="#2196F3" />
                <StatBar label="에너지" value={pet.energy} color="#4CAF50" />
                <StatBar label="스트레스" value={pet.stress} color="#9E9E9E" />
            </div>

            <div style={styles.expBox}>
                <span style={styles.expLabel}>경험치 ({pet.exp}/{pet.maxExp})</span>
                <div style={styles.expBg}>
                    <div style={{...styles.expFill, width: `${(pet.exp / pet.maxExp) * 100}%`}} />
                </div>
            </div>

            <PetControls isSleeping={pet.isSleeping} actions={actions} />
        </div>
    );
};

const StatBar = ({ label, value, color }) => (
    <div style={styles.statRow}>
        <span style={styles.statLabel}>{label}</span>
        <div style={styles.statBg}>
            <div style={{...styles.statFill, width: `${Math.min(100, value)}%`, backgroundColor: color}} />
        </div>
        <span style={styles.statVal}>{value}</span>
    </div>
);

const styles = {
    container: { maxWidth: '400px', margin: '0 auto', padding: '20px', background: '#fff', borderRadius: '15px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)' },
    header: { textAlign: 'center', marginBottom: '15px' },
    name: { margin: 0, fontSize: '24px' },
    badge: { fontSize: '14px', background: '#FFC107', padding: '2px 8px', borderRadius: '10px' },
    msg: { color: '#666', fontStyle: 'italic', marginTop: '5px' },
    scene: { height: '250px', display: 'flex', justifyContent: 'center', alignItems: 'center', background: '#f4f6f8', borderRadius: '15px', marginBottom: '20px', position: 'relative' },
    img: { maxWidth: '180px', transition: 'filter 0.3s' },
    zzz: { position: 'absolute', top: '20px', right: '40px', fontSize: '30px', fontWeight: 'bold', color: '#3F51B5' },
    stats: { marginBottom: '20px' },
    statRow: { display: 'flex', alignItems: 'center', marginBottom: '8px', fontSize: '14px' },
    statLabel: { width: '60px', fontWeight: 'bold' },
    statBg: { flex: 1, height: '8px', background: '#eee', borderRadius: '4px', margin: '0 10px', overflow: 'hidden' },
    statFill: { height: '100%', transition: 'width 0.3s' },
    statVal: { width: '30px', textAlign: 'right', fontSize: '12px' },
    expBox: { marginBottom: '20px' },
    expLabel: { fontSize: '12px', color: '#666', display: 'block', textAlign: 'right', marginBottom: '4px' },
    expBg: { height: '6px', background: '#eee', borderRadius: '3px' },
    expFill: { height: '100%', background: '#9C27B0', borderRadius: '3px', transition: 'width 0.3s' }
};

export default PetScreen;