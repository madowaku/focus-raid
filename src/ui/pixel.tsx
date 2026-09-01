type PixelProps = {
  className?: string;
  title?: string;
};

function SvgShell({ className, title, children, viewBox = '0 0 24 24' }: PixelProps & { children: React.ReactNode; viewBox?: string }) {
  return (
    <svg
      className={`pixel-art ${className ?? ''}`.trim()}
      viewBox={viewBox}
      role={title ? 'img' : 'presentation'}
      aria-hidden={title ? undefined : true}
      aria-label={title}
      shapeRendering="crispEdges"
    >
      {children}
    </svg>
  );
}

export function PixelRag(props: PixelProps) {
  return (
    <SvgShell {...props} title={props.title ?? 'ラグ'}>
      <rect x="7" y="3" width="2" height="3" fill="#f2c46d" />
      <rect x="15" y="2" width="2" height="4" fill="#f2c46d" />
      <rect x="8" y="5" width="9" height="8" fill="#d95b45" />
      <rect x="6" y="7" width="3" height="4" fill="#d95b45" />
      <rect x="10" y="8" width="2" height="2" fill="#f6ead4" />
      <rect x="14" y="7" width="2" height="2" fill="#172033" />
      <rect x="9" y="12" width="7" height="6" fill="#e2714e" />
      <rect x="11" y="13" width="3" height="4" fill="#f0c277" />
      <rect x="5" y="12" width="4" height="2" fill="#9a4762" />
      <rect x="16" y="11" width="4" height="2" fill="#9a4762" />
      <rect x="16" y="15" width="5" height="2" fill="#d95b45" />
      <rect x="19" y="17" width="3" height="2" fill="#d95b45" />
      <rect x="8" y="18" width="3" height="3" fill="#8c4b3e" />
      <rect x="14" y="18" width="3" height="3" fill="#8c4b3e" />
      <rect x="5" y="10" width="2" height="5" fill="#77543b" />
      <rect x="4" y="9" width="4" height="2" fill="#b78a52" />
    </SvgShell>
  );
}

export function PixelTower(props: PixelProps) {
  return (
    <SvgShell {...props} title={props.title ?? '天空塔'}>
      <rect x="11" y="1" width="2" height="3" fill="#d9e7f5" />
      <rect x="9" y="4" width="6" height="3" fill="#7f9ab6" />
      <rect x="8" y="7" width="8" height="5" fill="#5e7896" />
      <rect x="7" y="12" width="10" height="9" fill="#405a78" />
      <rect x="10" y="6" width="1" height="2" fill="#6df3df" />
      <rect x="13" y="9" width="1" height="2" fill="#6df3df" />
      <rect x="10" y="14" width="1" height="2" fill="#6df3df" />
      <rect x="14" y="16" width="1" height="2" fill="#6df3df" />
      <rect x="5" y="20" width="14" height="2" fill="#27384f" />
      <rect x="2" y="8" width="4" height="2" fill="#e9f0f8" opacity="0.88" />
      <rect x="17" y="6" width="5" height="2" fill="#e9f0f8" opacity="0.88" />
    </SvgShell>
  );
}

export function PixelAbyss(props: PixelProps) {
  return (
    <SvgShell {...props} title={props.title ?? '深層迷宮'}>
      <rect x="2" y="3" width="20" height="18" fill="#16132c" />
      <rect x="3" y="5" width="4" height="3" fill="#31265e" />
      <rect x="17" y="4" width="4" height="4" fill="#31265e" />
      <rect x="5" y="13" width="3" height="6" fill="#5c35a8" />
      <rect x="6" y="10" width="1" height="4" fill="#9d6cff" />
      <rect x="10" y="11" width="4" height="8" fill="#4b2b88" />
      <rect x="11" y="8" width="2" height="4" fill="#9d6cff" />
      <rect x="16" y="14" width="3" height="5" fill="#5c35a8" />
      <rect x="17" y="11" width="1" height="4" fill="#b694ff" />
      <rect x="8" y="19" width="9" height="2" fill="#231943" />
    </SvgShell>
  );
}

export function PixelBoss(props: PixelProps) {
  return (
    <SvgShell {...props} title={props.title ?? 'WORLD BOSS'}>
      <rect x="5" y="5" width="14" height="10" fill="#9e3b36" />
      <rect x="7" y="3" width="3" height="4" fill="#e0a44e" />
      <rect x="15" y="2" width="3" height="5" fill="#e0a44e" />
      <rect x="3" y="8" width="4" height="3" fill="#6b2d3d" />
      <rect x="18" y="7" width="4" height="4" fill="#6b2d3d" />
      <rect x="8" y="8" width="2" height="2" fill="#ffd56a" />
      <rect x="15" y="8" width="2" height="2" fill="#ffd56a" />
      <rect x="10" y="12" width="6" height="2" fill="#351b2c" />
      <rect x="7" y="15" width="4" height="5" fill="#7d3132" />
      <rect x="15" y="15" width="4" height="5" fill="#7d3132" />
      <rect x="2" y="19" width="20" height="2" fill="#351b2c" />
    </SvgShell>
  );
}
