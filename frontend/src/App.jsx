import { useState } from 'react';
import './App.css';

function App() {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState(null);

  const handleValidate = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/sql/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query }),
      });
      const data = await response.json();
      setResult(data);
    } catch (error) {
      setResult({ valid: false, message: 'Erro ao conectar com o servidor Backend.' });
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial' }}>
      <h1>Processador de Consultas</h1>
      <h2>HU1 - Entrada e Validação da Consulta</h2>
      
      <textarea 
        rows="6" 
        cols="80" 
        value={query} 
        onChange={(e) => setQuery(e.target.value)} 
        placeholder="Digite sua consulta SQL aqui (ex: SELECT * FROM cliente)"
        style={{ width: '100%', fontSize: '16px', padding: '10px' }}
      />
      <br /><br />
      
      <button 
        onClick={handleValidate} 
        style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px' }}>
        Validar Consulta
      </button>

      {result && (
        <div style={{ marginTop: '20px', padding: '15px', borderRadius: '5px', backgroundColor: result.valid ? '#d4edda' : '#f8d7da', color: result.valid ? '#155724' : '#721c24' }}>
          <strong>{result.valid ? '✅ Sucesso:' : '❌ Erro:'}</strong> {result.message}
        </div>
      )}
    </div>
  );
}

export default App;
