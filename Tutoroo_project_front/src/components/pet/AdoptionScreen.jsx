import React, { useEffect } from 'react';

const AdoptionScreen = ({ adoptableList, onFetch, onAdopt }) => {
    useEffect(() => {
        onFetch();
    }, [onFetch]);

    return (
        <div style={styles.container}>
            <h2>새로운 파트너를 선택하세요</h2>
            <div style={styles.list}>
                {adoptableList.map((pet) => (
                    <div key={pet.type} style={styles.card}>
                        <div style={styles.icon}>🐾</div>
                        <h3>{pet.name}</h3>
                        <p>{pet.description}</p>
                        <button onClick={() => onAdopt(pet.type)} style={styles.btn}>
                            입양하기
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};

const styles = {
    container: { textAlign: 'center', padding: '40px' },
    list: { display: 'flex', justifyContent: 'center', gap: '20px', flexWrap: 'wrap' },
    card: { width: '220px', padding: '20px', border: '1px solid #eee', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' },
    icon: { fontSize: '40px', marginBottom: '10px' },
    btn: { marginTop: '15px', width: '100%', padding: '10px', background: '#4CAF50', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }
};

export default AdoptionScreen;