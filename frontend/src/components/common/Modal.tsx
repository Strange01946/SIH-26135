import * as Dialog from '@radix-ui/react-dialog';
import { useRef, type ReactNode } from 'react';

export function Modal({ open, onClose, title, children, className, testId }: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  className?: string;
  testId?: string;
}) {
  const returnFocus = useRef<HTMLElement | null>(null);
  return (
    <Dialog.Root open={open} onOpenChange={(next) => { if (!next) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50" />
        <Dialog.Content
          aria-describedby={undefined}
          data-testid={testId}
          className={`fixed top-1/2 left-1/2 z-50 max-h-[90vh] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-xl bg-white shadow-xl ${className ?? ''}`}
          onOpenAutoFocus={() => {
            returnFocus.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
          }}
          onCloseAutoFocus={(event) => {
            event.preventDefault();
            returnFocus.current?.focus();
          }}
        >
          <div className="flex items-center justify-between gap-3 border-b border-gray-200 px-6 py-4">
            <Dialog.Title className="text-lg font-bold text-navy">{title}</Dialog.Title>
            <Dialog.Close aria-label="Close dialog" className="rounded px-2 text-xl text-gray-600 focus-visible:outline-2">
              ×
            </Dialog.Close>
          </div>
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
