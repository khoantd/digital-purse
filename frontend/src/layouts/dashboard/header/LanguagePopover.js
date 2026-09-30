import { useState } from 'react';
import { IconButton, MenuItem, Popover, Stack, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import Iconify from '../../../components/iconify';
import { displayCodeForLang } from '../../../i18n/storage';

const LANG_OPTIONS = [
  { code: 'vi', labelKey: 'vietnamese' },
  { code: 'en', labelKey: 'english' },
];

export default function LanguagePopover() {
  const { t, i18n } = useTranslation('language');
  const [open, setOpen] = useState(null);
  const current = i18n.language?.startsWith('en') ? 'en' : 'vi';

  const handleOpen = (event) => {
    setOpen(event.currentTarget);
  };

  const handleClose = () => {
    setOpen(null);
  };

  const handleSelect = (code) => {
    i18n.changeLanguage(code);
    handleClose();
  };

  return (
    <>
      <IconButton
        onClick={handleOpen}
        aria-label={t('selectLanguage')}
        sx={{
          px: 1,
          py: 0.75,
          borderRadius: 1,
          cursor: 'pointer',
          gap: 0.5,
          ...(open && {
            bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.action.selectedOpacity),
          }),
        }}
      >
        <Iconify icon="eva:globe-outline" width={22} height={22} />
        <Typography variant="subtitle2" component="span" sx={{ fontWeight: 700, lineHeight: 1 }}>
          {displayCodeForLang(current)}
        </Typography>
      </IconButton>
      <Popover
        open={Boolean(open)}
        anchorEl={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{
          sx: {
            p: 1,
            mt: 1,
            minWidth: 180,
            '& .MuiMenuItem-root': {
              typography: 'body2',
              borderRadius: 0.75,
              cursor: 'pointer',
            },
          },
        }}
      >
        <Stack spacing={0.5}>
          {LANG_OPTIONS.map((opt) => {
            const active = current === opt.code;
            return (
              <MenuItem
                key={opt.code}
                onClick={() => handleSelect(opt.code)}
                selected={active}
                sx={{ cursor: 'pointer', justifyContent: 'space-between', gap: 1 }}
              >
                <Typography variant="body2">{t(opt.labelKey)}</Typography>
                {active ? <Iconify icon="eva:checkmark-fill" width={18} height={18} /> : null}
              </MenuItem>
            );
          })}
        </Stack>
      </Popover>
    </>
  );
}
