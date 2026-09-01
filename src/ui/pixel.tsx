import type { ReactNode } from 'react';

type PixelProps = {
  className?: string;
  title?: string;
};

export type RagPose = 'idle' | 'depart' | 'return' | 'raid';

type RagProps = PixelProps & {
  pose?: RagPose;
};

function SvgShell({ className, title, children, viewBox = '0 0 24 24' }: PixelProps & { children: ReactNode; viewBox?: string }) {
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

const rag = {
  outline: '#3a2330',
  body: '#ef4a35',
  bodyLight: '#ff7352',
  bodyShadow: '#b9323a',
  belly: '#ffd58a',
  bellyLight: '#fff0bf',
  horn: '#ffe08a',
  hornShadow: '#d79a52',
  wing: '#d93b42',
  wingMembrane: '#f38e61',
  bag: '#8b573a',
  bagLight: '#bd7b4c',
  bagDark: '#5f3a31',
  eye: '#121725',
  eyeLight: '#fff8da',
  spear: '#d9ecff',
  spearShadow: '#8ba9c4',
  spearWood: '#8a5b3c',
  dust: '#b9c6d8',
};

function RagFace() {
  return (
    <>
      <rect x="10" y="7" width="11" height="8" fill={rag.outline} />
      <rect x="9" y="8" width="11" height="7" fill={rag.body} />
      <rect x="11" y="7" width="8" height="2" fill={rag.bodyLight} />
      <rect x="8" y="10" width="3" height="4" fill={rag.body} />
      <rect x="9" y="13" width="8" height="3" fill={rag.bellyLight} />
      <rect x="17" y="10" width="2" height="2" fill={rag.eye} />
      <rect x="18" y="10" width="1" height="1" fill={rag.eyeLight} />
      <rect x="12" y="10" width="1" height="1" fill={rag.bodyShadow} />
      <rect x="10" y="4" width="3" height="4" fill={rag.outline} />
      <rect x="11" y="3" width="2" height="5" fill={rag.horn} />
      <rect x="12" y="3" width="1" height="2" fill={rag.hornShadow} />
      <rect x="17" y="3" width="3" height="5" fill={rag.outline} />
      <rect x="18" y="2" width="2" height="6" fill={rag.horn} />
      <rect x="19" y="2" width="1" height="3" fill={rag.hornShadow} />
    </>
  );
}

function RagBody({ lowered = false }: { lowered?: boolean }) {
  const y = lowered ? 18 : 16;
  return (
    <>
      <rect x="11" y={y} width="9" height="9" fill={rag.outline} />
      <rect x="12" y={y} width="8" height="8" fill={rag.body} />
      <rect x="12" y={y + 2} width="4" height="5" fill={rag.belly} />
      <rect x="13" y={y + 2} width="2" height="4" fill={rag.bellyLight} />
      <rect x="9" y={y + 2} width="4" height="3" fill={rag.outline} />
      <rect x="9" y={y + 2} width="3" height="2" fill={rag.bodyShadow} />
      <rect x="19" y={y + 1} width="5" height="3" fill={rag.outline} />
      <rect x="19" y={y + 1} width="4" height="2" fill={rag.body} />
      <rect x="22" y={y + 3} width="5" height="3" fill={rag.outline} />
      <rect x="22" y={y + 3} width="4" height="2" fill={rag.body} />
      <rect x="26" y={y + 5} width="3" height="2" fill={rag.bodyShadow} />
    </>
  );
}

function RagWing({ open = false }: { open?: boolean }) {
  if (open) {
    return (
      <>
        <rect x="19" y="14" width="6" height="2" fill={rag.outline} />
        <rect x="21" y="12" width="7" height="2" fill={rag.outline} />
        <rect x="24" y="10" width="5" height="2" fill={rag.outline} />
        <rect x="20" y="15" width="5" height="2" fill={rag.wing} />
        <rect x="22" y="13" width="5" height="2" fill={rag.wingMembrane} />
        <rect x="25" y="11" width="3" height="2" fill={rag.wingMembrane} />
      </>
    );
  }

  return (
    <>
      <rect x="19" y="15" width="6" height="3" fill={rag.outline} />
      <rect x="21" y="13" width="4" height="3" fill={rag.outline} />
      <rect x="20" y="16" width="4" height="2" fill={rag.wing} />
      <rect x="22" y="14" width="2" height="3" fill={rag.wingMembrane} />
    </>
  );
}

function RagBag({ low = false }: { low?: boolean }) {
  const y = low ? 19 : 15;
  return (
    <>
      <rect x="5" y={y} width="7" height="8" fill={rag.outline} />
      <rect x="6" y={y + 1} width="6" height="6" fill={rag.bag} />
      <rect x="7" y={y} width="4" height="2" fill={rag.bagLight} />
      <rect x="6" y={y + 5} width="6" height="2" fill={rag.bagDark} />
      <rect x="8" y={y + 2} width="2" height="1" fill={rag.bagLight} />
    </>
  );
}

function RagIdle() {
  return (
    <>
      <RagFace />
      <RagBody />
      <RagWing />
      <RagBag />
      <rect x="12" y="24" width="4" height="5" fill={rag.outline} />
      <rect x="13" y="24" width="3" height="4" fill={rag.bodyShadow} />
      <rect x="18" y="24" width="4" height="5" fill={rag.outline} />
      <rect x="18" y="24" width="3" height="4" fill={rag.bodyShadow} />
      <rect x="12" y="28" width="5" height="2" fill={rag.bagDark} />
      <rect x="18" y="28" width="5" height="2" fill={rag.bagDark} />
    </>
  );
}

function RagDepart() {
  return (
    <g transform="translate(1 0)">
      <RagFace />
      <RagBody />
      <RagWing open />
      <RagBag />
      <rect x="11" y="24" width="4" height="4" fill={rag.outline} />
      <rect x="12" y="24" width="3" height="3" fill={rag.bodyShadow} />
      <rect x="18" y="23" width="4" height="5" fill={rag.outline} />
      <rect x="19" y="23" width="3" height="4" fill={rag.bodyShadow} />
      <rect x="9" y="27" width="7" height="2" fill={rag.bagDark} />
      <rect x="19" y="28" width="6" height="2" fill={rag.bagDark} />
      <rect x="4" y="28" width="3" height="2" fill={rag.dust} opacity=".55" />
      <rect x="1" y="29" width="2" height="1" fill={rag.dust} opacity=".32" />
    </g>
  );
}

function RagReturn() {
  return (
    <g transform="translate(0 2)">
      <g transform="translate(0 2)">
        <RagFace />
      </g>
      <RagBody lowered />
      <RagWing />
      <RagBag low />
      <rect x="11" y="26" width="5" height="3" fill={rag.outline} />
      <rect x="12" y="26" width="4" height="2" fill={rag.bodyShadow} />
      <rect x="17" y="27" width="6" height="3" fill={rag.outline} />
      <rect x="18" y="27" width="5" height="2" fill={rag.bodyShadow} />
      <rect x="24" y="7" width="2" height="2" fill="#8dc8ff" opacity=".65" />
      <rect x="26" y="5" width="1" height="1" fill="#bfe6ff" opacity=".75" />
    </g>
  );
}

function RagRaid() {
  return (
    <>
      <RagFace />
      <RagBody />
      <RagWing open />
      <RagBag />
      <rect x="12" y="24" width="4" height="5" fill={rag.outline} />
      <rect x="18" y="24" width="4" height="5" fill={rag.outline} />
      <rect x="12" y="28" width="5" height="2" fill={rag.bagDark} />
      <rect x="18" y="28" width="5" height="2" fill={rag.bagDark} />
      <rect x="24" y="4" width="2" height="18" fill={rag.spearWood} />
      <rect x="23" y="3" width="4" height="4" fill={rag.spearShadow} />
      <rect x="24" y="1" width="2" height="4" fill={rag.spear} />
      <rect x="25" y="0" width="1" height="2" fill={rag.eyeLight} />
      <rect x="21" y="19" width="4" height="3" fill={rag.bodyShadow} />
      <rect x="28" y="8" width="2" height="2" fill="#ffd36a" />
      <rect x="30" y="6" width="1" height="1" fill="#fff2a5" />
    </>
  );
}

export function PixelRag({ pose = 'idle', className, title }: RagProps) {
  const label = title ?? `ラグ BABY ${pose.toUpperCase()}`;
  return (
    <SvgShell className={`rag-sprite rag-pose-${pose} ${className ?? ''}`.trim()} title={label} viewBox="0 0 32 32">
      {pose === 'idle' && <RagIdle />}
      {pose === 'depart' && <RagDepart />}
      {pose === 'return' && <RagReturn />}
      {pose === 'raid' && <RagRaid />}
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
