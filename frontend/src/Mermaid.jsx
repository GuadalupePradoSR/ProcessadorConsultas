import React, { useEffect, useState } from 'react';
import mermaid from 'mermaid';

mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
});

const Mermaid = ({ chart, id }) => {
  const [svg, setSvg] = useState('');

  useEffect(() => {
    if (chart) {
      mermaid.render(id, chart).then(({ svg }) => {
        setSvg(svg);
      }).catch((e) => {
        console.error(e);
        setSvg(`<p style="color: red;">Erro ao gerar grafo</p>`);
      });
    }
  }, [chart, id]);

  return <div dangerouslySetInnerHTML={{ __html: svg }} />;
};

export default Mermaid;
