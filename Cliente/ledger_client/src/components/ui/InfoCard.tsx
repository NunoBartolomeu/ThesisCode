'use client';

interface InfoSection {
  title: string;
  icon?: React.ReactNode;
  items: Array<{
    label: string;
    value: string | number;
    type?: 'hash' | 'normal';
  }>;
}

interface InfoCardProps {
  title: string;
  sections: InfoSection[];
}

export function InfoCard({ title, sections }: InfoCardProps) {
  return (
    <div className="rounded-2xl shadow-xl border-2 overflow-hidden" style={{ background: 'var(--gradient)', borderColor: 'var(--border)' }}>
      <div className="px-6 py-6 border-b-2" style={{ borderColor: 'var(--border)' }}>
        <h2 className="text-2xl font-bold" style={{ color: 'var(--text)' }}>
          {title}
        </h2>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6">
        {sections.map((section, index) => (
          <div key={index}>
            <div className="flex items-center space-x-2 border-b pb-2 mb-3">
              {section.icon}
              <h3 className="text-lg font-semibold" style={{ color: 'var(--text)' }}>
                {section.title}
              </h3>
            </div>
            <div className="space-y-2">
              {section.items.map((item, itemIndex) => (
                <p key={itemIndex} style={{ color: 'var(--text-muted)' }}>
                  {item.label}: <span 
                    style={{ 
                      color: 'var(--text)', 
                      fontSize: item.type === 'hash' ? '0.8rem' : 'inherit',
                      wordBreak: item.type === 'hash' ? 'break-all' : 'normal'
                    }}
                  >
                    {item.value}
                  </span>
                </p>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}